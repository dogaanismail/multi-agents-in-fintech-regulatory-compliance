package org.banksolution.servicesecurity.probe;

import org.banksolution.servicesecurity.feign.ServiceTokenFeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "probe", url = "${probe.url}", configuration = ServiceTokenFeignConfiguration.class)
public interface ProbeClient {

    @GetMapping("/downstream")
    String callDownstream();
}
