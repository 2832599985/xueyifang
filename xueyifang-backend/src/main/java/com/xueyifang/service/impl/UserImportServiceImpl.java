package com.xueyifang.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xueyifang.common.ErrorCode;
import com.xueyifang.constant.UserConstant;
import com.xueyifang.domain.dto.admin.UserImportRowDTO;
import com.xueyifang.domain.po.Professional;
import com.xueyifang.domain.po.User;
import com.xueyifang.domain.vo.admin.UserImportResultVO;
import com.xueyifang.exception.BusinessException;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.IProfessionalService;
import com.xueyifang.service.IUserImportService;
import com.xueyifang.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 用户批量导入服务实现
 *
 * @author xueyifang
 */
@Service
@Slf4j
public class UserImportServiceImpl implements IUserImportService {

    /**
     * 密码加密盐值，从配置文件读取
     */
    @Value("${password.salt:xueyifang}")
    private String passwordSalt;

    /**
     * 用户昵称前缀
     */
    @Value("${xueyifang.user.nickname-prefix:xueyifang}")
    private String nicknamePrefix;

    @Resource
    private IUserService userService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private IProfessionalService professionalService;

    /**
     * 密码正则：8-16位，需包含数字和字母
     */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z]).{8,16}$");

    /**
     * 手机号正则
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /**
     * 邮箱正则
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$");

    /**
     * Excel 列名到字段名映射（支持中文和英文列名）
     */
    private static final Map<String, String> COLUMN_MAPPING = new LinkedHashMap<>();

    static {
        COLUMN_MAPPING.put("studentId", "studentId");
        COLUMN_MAPPING.put("学号", "studentId");
        COLUMN_MAPPING.put("realName", "realName");
        COLUMN_MAPPING.put("姓名", "realName");
        COLUMN_MAPPING.put("真实姓名", "realName");
        COLUMN_MAPPING.put("phone", "phone");
        COLUMN_MAPPING.put("手机号", "phone");
        COLUMN_MAPPING.put("电话", "phone");
        COLUMN_MAPPING.put("professionalId", "professionalId");
        COLUMN_MAPPING.put("专业ID", "professionalId");
        COLUMN_MAPPING.put("password", "password");
        COLUMN_MAPPING.put("密码", "password");
        COLUMN_MAPPING.put("email", "email");
        COLUMN_MAPPING.put("邮箱", "email");
        COLUMN_MAPPING.put("dormitory", "dormitory");
        COLUMN_MAPPING.put("寝室", "dormitory");
        COLUMN_MAPPING.put("寝室号", "dormitory");
        COLUMN_MAPPING.put("grade", "grade");
        COLUMN_MAPPING.put("年级", "grade");
        COLUMN_MAPPING.put("nickname", "nickname");
        COLUMN_MAPPING.put("昵称", "nickname");
    }

    /**
     * 模板列顺序
     */
    public static final String[] TEMPLATE_COLUMNS = {
            "studentId", "realName", "phone", "professionalId", "password",
            "email", "dormitory", "grade", "nickname"
    };

    @Override
    public UserImportResultVO importUsers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名无效");
        }

        String suffix = FileUtil.getSuffix(originalFilename).toLowerCase();
        log.info("开始批量导入用户，文件名: {}，类型: {}", originalFilename, suffix);

        List<UserImportRowDTO> rows;
        try {
            if ("xlsx".equals(suffix) || "xls".equals(suffix)) {
                rows = parseExcel(file);
            } else if ("csv".equals(suffix)) {
                rows = parseCsv(file);
            } else {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 .xlsx、.xls、.csv 格式文件");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析文件失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件解析失败: " + e.getMessage());
        }

        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件中没有有效数据");
        }

        // 预加载已存在的学号
        Set<String> existingStudentIds = loadExistingStudentIds();

        // 预加载专业ID
        Set<Long> validProfessionalIds = loadValidProfessionalIds();

        UserImportResultVO result = new UserImportResultVO();

        for (UserImportRowDTO row : rows) {
            try {
                // 校验行数据
                String validateError = validateRow(row, validProfessionalIds);
                if (validateError != null) {
                    // 如果是说明行（学号包含“必填”字样），则直接跳过，不计入失败
                    if (row.getStudentId() != null && row.getStudentId().contains("必填")) {
                        continue;
                    }
                    result.addFailed(row.getRowNum(), row.getStudentId(), validateError);
                    continue;
                }

                // 检查学号是否已存在
                if (existingStudentIds.contains(row.getStudentId())) {
                    result.addSkipped();
                    continue;
                }

                // 创建用户
                User user = createUserFromRow(row);
                boolean saved = userService.save(user);

                if (saved) {
                    result.addSuccess();
                    // 添加到已存在集合，避免同一批次内重复导入
                    existingStudentIds.add(row.getStudentId());
                } else {
                    result.addFailed(row.getRowNum(), row.getStudentId(), "保存失败");
                }
            } catch (Exception e) {
                log.error("导入行 {} 失败", row.getRowNum(), e);
                result.addFailed(row.getRowNum(), row.getStudentId(), "系统错误: " + e.getMessage());
            }
        }

        log.info("批量导入完成，总数: {}，成功: {}，跳过: {}，失败: {}",
                result.getTotalCount(), result.getSuccessCount(),
                result.getSkippedCount(), result.getFailedCount());

        return result;
    }

    /**
     * 解析 Excel 文件
     */
    private List<UserImportRowDTO> parseExcel(MultipartFile file) throws IOException {
        List<UserImportRowDTO> rows = new ArrayList<>();

        try (InputStream is = file.getInputStream()) {
            ExcelReader reader = ExcelUtil.getReader(is);
            // 读取表头
            List<Object> headers = reader.readRow(0);
            if (headers == null || headers.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "Excel 表头为空");
            }

            // 映射列索引
            Map<Integer, String> columnIndexToField = mapColumnsToFields(headers);
            if (!columnIndexToField.containsValue("studentId") ||
                    !columnIndexToField.containsValue("realName") ||
                    !columnIndexToField.containsValue("phone") ||
                    !columnIndexToField.containsValue("professionalId") ||
                    !columnIndexToField.containsValue("password")) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,
                        "Excel 缺少必填列（studentId/学号, realName/姓名, phone/手机号, professionalId/专业ID, password/密码）");
            }

            // 读取数据行
            List<List<Object>> dataRows = reader.read(1); // 从第2行开始
            int rowNum = 2; // Excel 行号从1开始，数据从第2行开始
            for (List<Object> dataRow : dataRows) {
                UserImportRowDTO dto = parseExcelRow(dataRow, columnIndexToField, rowNum);
                if (dto != null) {
                    rows.add(dto);
                }
                rowNum++;
            }
        }

        return rows;
    }

    /**
     * 解析 CSV 文件
     */
    private List<UserImportRowDTO> parseCsv(MultipartFile file) throws IOException {
        List<UserImportRowDTO> rows = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            CsvReader reader = CsvUtil.getReader();
            CsvData csvData = reader.read(br);
            List<CsvRow> csvRows = csvData.getRows();

            if (csvRows.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "CSV 文件内容为空");
            }

            // 第一行是表头
            CsvRow headerRow = csvRows.get(0);
            List<Object> headerList = new ArrayList<>(headerRow.getRawList());
            Map<Integer, String> columnIndexToField = mapColumnsToFields(headerList);

            if (!columnIndexToField.containsValue("studentId") ||
                    !columnIndexToField.containsValue("realName") ||
                    !columnIndexToField.containsValue("phone") ||
                    !columnIndexToField.containsValue("professionalId") ||
                    !columnIndexToField.containsValue("password")) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,
                        "CSV 缺少必填列（studentId/学号, realName/姓名, phone/手机号, professionalId/专业ID, password/密码）");
            }

            // 解析数据行
            for (int i = 1; i < csvRows.size(); i++) {
                CsvRow dataRow = csvRows.get(i);
                List<Object> dataList = new ArrayList<>(dataRow.getRawList());
                UserImportRowDTO dto = parseExcelRow(dataList, columnIndexToField, i + 1);
                if (dto != null) {
                    rows.add(dto);
                }
            }
        }

        return rows;
    }

    /**
     * 映射列名到字段名
     */
    private Map<Integer, String> mapColumnsToFields(List<Object> headers) {
        Map<Integer, String> mapping = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            Object header = headers.get(i);
            if (header != null) {
                String headerStr = header.toString().replace("\uFEFF", "").trim();
                String fieldName = COLUMN_MAPPING.get(headerStr);
                if (fieldName != null) {
                    mapping.put(i, fieldName);
                }
            }
        }
        return mapping;
    }

    /**
     * 解析 Excel/CSV 行数据
     */
    private UserImportRowDTO parseExcelRow(List<Object> dataRow, Map<Integer, String> columnMapping, int rowNum) {
        if (dataRow == null || dataRow.isEmpty()) {
            return null;
        }

        // 检查是否全部为空
        boolean allEmpty = dataRow.stream().allMatch(obj -> obj == null || obj.toString().trim().isEmpty());
        if (allEmpty) {
            return null;
        }

        UserImportRowDTO dto = new UserImportRowDTO();
        dto.setRowNum(rowNum);

        for (Map.Entry<Integer, String> entry : columnMapping.entrySet()) {
            int colIndex = entry.getKey();
            String fieldName = entry.getValue();

            if (colIndex >= dataRow.size()) {
                continue;
            }

            Object value = dataRow.get(colIndex);
            String strValue = value == null ? "" : value.toString().trim();

            switch (fieldName) {
                case "studentId":
                    dto.setStudentId(strValue);
                    break;
                case "realName":
                    dto.setRealName(strValue);
                    break;
                case "phone":
                    dto.setPhone(strValue);
                    break;
                case "professionalId":
                    if (StringUtils.isNotBlank(strValue)) {
                        try {
                            dto.setProfessionalId(Long.parseLong(strValue.replaceAll("\\.0$", "")));
                        } catch (NumberFormatException e) {
                            // 忽略，后续校验会捕获
                        }
                    }
                    break;
                case "password":
                    dto.setPassword(strValue);
                    break;
                case "email":
                    dto.setEmail(strValue);
                    break;
                case "dormitory":
                    dto.setDormitory(strValue);
                    break;
                case "grade":
                    dto.setGrade(strValue);
                    break;
                case "nickname":
                    dto.setNickname(strValue);
                    break;
                default:
                    break;
            }
        }

        return dto;
    }

    /**
     * 校验行数据
     *
     * @return 错误信息，null 表示校验通过
     */
    private String validateRow(UserImportRowDTO row, Set<Long> validProfessionalIds) {
        // 必填字段校验
        if (StringUtils.isBlank(row.getStudentId())) {
            return "学号不能为空";
        }
        if (row.getStudentId().length() < 6 || row.getStudentId().length() > 20) {
            return "学号长度应在6-20位之间";
        }
        if (StringUtils.isBlank(row.getRealName())) {
            return "真实姓名不能为空";
        }
        if (row.getRealName().length() < 2 || row.getRealName().length() > 20) {
            return "真实姓名长度应在2-20位之间";
        }
        if (StringUtils.isBlank(row.getPhone())) {
            return "手机号不能为空";
        }
        if (!PHONE_PATTERN.matcher(row.getPhone()).matches()) {
            return "手机号格式不正确";
        }
        if (row.getProfessionalId() == null) {
            return "专业ID不能为空";
        }
        if (!validProfessionalIds.contains(row.getProfessionalId())) {
            return "专业ID不存在: " + row.getProfessionalId();
        }
        if (StringUtils.isBlank(row.getPassword())) {
            return "密码不能为空";
        }
        if (!PASSWORD_PATTERN.matcher(row.getPassword()).matches()) {
            return "密码必须为8-16位，且包含数字和字母";
        }

        // 可选字段校验
        if (StringUtils.isNotBlank(row.getEmail())) {
            if (!EMAIL_PATTERN.matcher(row.getEmail()).matches()) {
                return "邮箱格式不正确";
            }
        }

        if (StringUtils.isNotBlank(row.getGrade())) {
            String grade = row.getGrade().trim();
            if (!grade.equals(UserConstant.GRADE_FRESHMAN) &&
                    !grade.equals(UserConstant.GRADE_SOPHOMORE) &&
                    !grade.equals(UserConstant.GRADE_JUNIOR) &&
                    !grade.equals(UserConstant.GRADE_SENIOR) &&
                    !grade.equals(UserConstant.GRADE_MASTER_1) &&
                    !grade.equals(UserConstant.GRADE_MASTER_2) &&
                    !grade.equals(UserConstant.GRADE_MASTER_3)) {
                return "年级只能是：大一、大二、大三、大四、研一、研二、研三";
            }
        }

        return null;
    }

    /**
     * 根据行数据创建用户对象
     */
    private User createUserFromRow(UserImportRowDTO row) {
        User user = new User();
        user.setStudentId(row.getStudentId());
        user.setPassword(encryptPassword(row.getPassword()));
        user.setRealName(row.getRealName());
        user.setPhone(row.getPhone());
        user.setProfessionalId(row.getProfessionalId());
        user.setEmail(row.getEmail());
        user.setDormitory(row.getDormitory());
        user.setGrade(row.getGrade());

        // 昵称：优先使用导入值，否则自动生成
        if (StringUtils.isNotBlank(row.getNickname())) {
            user.setNickname(row.getNickname());
        } else {
            user.setNickname(generateUniqueNickname());
        }

        // 设置默认值（管理员导入的用户直接拥有发布权限）
        user.setCampusId(1L);
        user.setRole(UserConstant.ROLE_STUDENT);
        user.setPublishPermission(UserConstant.HAS_PUBLISH_PERMISSION);  // 管理员导入直接有发布权限
        user.setPermissionReviewStatus(UserConstant.PERMISSION_APPROVED); // 审核状态为已通过
        user.setWalletBalance(BigDecimal.ZERO);
        user.setFrozenAmount(BigDecimal.ZERO);
        user.setSellerDeposit(BigDecimal.ZERO);
        user.setCreditScore(100);
        user.setEduVerified(0);
        user.setAccountStatus(UserConstant.ACCOUNT_NORMAL);

        return user;
    }

    /**
     * 密码加密（生产级 BCrypt 随机盐哈希）
     */
    private String encryptPassword(String password) {
        return com.xueyifang.utils.PasswordUtils.hashPassword(password);
    }

    /**
     * 生成唯一昵称
     */
    private String generateUniqueNickname() {
        String uniqueId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return nicknamePrefix + "_" + uniqueId;
    }

    /**
     * 加载数据库中已存在的学号
     */
    private Set<String> loadExistingStudentIds() {
        List<User> users = userMapper.selectList(
                new QueryWrapper<User>().select("student_id").eq("is_delete", 0)
        );
        Set<String> studentIds = new HashSet<>();
        for (User user : users) {
            studentIds.add(user.getStudentId());
        }
        return studentIds;
    }

    /**
     * 加载有效的专业ID
     */
    private Set<Long> loadValidProfessionalIds() {
        List<Professional> professionals = professionalService.list();
        Set<Long> ids = new HashSet<>();
        for (Professional p : professionals) {
            ids.add(p.getId());
        }
        return ids;
    }
}
