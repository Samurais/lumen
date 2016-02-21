package org.lskk.lumen.reasoner.intent;

import org.apache.camel.CamelContext;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Produce;
import org.apache.camel.builder.LoggingErrorHandlerBuilder;
import org.apache.camel.builder.ProxyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.BeanInvocation;
import org.apache.camel.language.Simple;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.lskk.lumen.core.LumenChannel;
import org.lskk.lumen.core.util.ToJson;
import org.lskk.lumen.persistence.rabbitmq.FactRequest;
import org.lskk.lumen.persistence.service.FactService;
import org.lskk.lumen.persistence.web.FactServiceOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * Created by ceefour on 21/02/2016.
 */
@Component
public class ServicesRouter extends RouteBuilder {

    @Inject
    private Environment env;
    @Inject
    private ToJson toJson;

    @Override
    public void configure() throws Exception {
        errorHandler(new LoggingErrorHandlerBuilder(log));
        from("direct:" + LumenChannel.PERSISTENCE_FACT.key())
                .process(exchange -> {
                    final BeanInvocation invocation = exchange.getIn().getBody(BeanInvocation.class);
                    final FactRequest factRequest = new FactRequest();
                    factRequest.setOperation(FactServiceOperation.valueOf(invocation.getMethod().getName()));
                    log.info("Invoking {} {}", invocation.getMethod(), invocation.getArgs());
                    for (int i = 0; i < invocation.getArgs().length; i++) {
                        final String simpleExpr = invocation.getMethod().getParameters()[i].getAnnotation(Simple.class).value();
                        final String paramName = StringUtils.substringAfter(simpleExpr, "body.");
                        PropertyUtils.setProperty(factRequest, paramName, invocation.getArgs()[i]);
                    }
                    exchange.getIn().setBody(factRequest);
                    exchange.setPattern(ExchangePattern.InOut);
                    //exchange.getIn().setHeader(RabbitMQConstants.REPLY_TO, );
                })
                .bean(toJson)
                .to("rabbitmq://localhost/amq.topic?connectionFactory=#amqpConnFactory&exchangeType=topic&autoDelete=false&skipQueueDeclare=true&routingKey=" +
                        LumenChannel.PERSISTENCE_FACT.key());
    }

    @Configuration
    public static class ServicesConfig {
        @Inject
        private CamelContext camel;

        @Bean
        public FactService factServiceProxy() throws Exception {
            return new ProxyBuilder(camel).binding(false).endpoint("direct:" + LumenChannel.PERSISTENCE_FACT.key()).build(FactService.class);
        }
    }

}
