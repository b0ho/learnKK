package com.learnkk.auth.dto;

import java.util.List;

/** Profile view combining identity fields and editable profile fields. */
public record ProfileResponse(
    String nickname, String employeeNo, List<String> tags, String intro) {}
