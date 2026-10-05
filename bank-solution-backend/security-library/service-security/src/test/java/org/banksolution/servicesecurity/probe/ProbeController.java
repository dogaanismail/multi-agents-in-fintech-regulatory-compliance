package org.banksolution.servicesecurity.probe;

import org.banksolution.servicesecurity.AuthenticatedCaller;
import org.banksolution.servicesecurity.Permissions;
import org.banksolution.servicesecurity.RequiresPermission;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProbeController {

    @GetMapping("/api/v1/probe")
    @RequiresPermission(Permissions.PAYMENT_READ)
    public String readProbe(@AuthenticationPrincipal Jwt jwt) {
        return AuthenticatedCaller.username(jwt);
    }

    @PostMapping("/api/v1/probe")
    @RequiresPermission(Permissions.PAYMENT_REVIEW)
    public String reviewProbe() {
        return "reviewed";
    }
}
