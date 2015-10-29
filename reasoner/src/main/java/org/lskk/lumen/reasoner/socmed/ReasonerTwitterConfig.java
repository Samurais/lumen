package org.lskk.lumen.reasoner.socmed;

import org.lskk.lumen.socmed.AgentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Created by ceefour on 29/10/2015.
 */
@Configuration
public class ReasonerTwitterConfig {

    @Bean
    public AgentRepository agentRepo() throws IOException {
        return new AgentRepository();
    }
}
