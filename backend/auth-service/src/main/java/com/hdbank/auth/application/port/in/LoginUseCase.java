package com.hdbank.auth.application.port.in;

import com.hdbank.auth.application.dto.LoginCommand;
import com.hdbank.auth.domain.valueobject.LoginResult;

public interface LoginUseCase {
    LoginResult login(LoginCommand command);
}
