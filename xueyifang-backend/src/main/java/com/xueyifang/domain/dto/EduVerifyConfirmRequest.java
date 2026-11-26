package com.xueyifang.domain.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class EduVerifyConfirmRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eduEmail;
    private String verifyCode;
}
