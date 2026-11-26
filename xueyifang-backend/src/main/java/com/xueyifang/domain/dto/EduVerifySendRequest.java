package com.xueyifang.domain.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class EduVerifySendRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eduEmail;
}
