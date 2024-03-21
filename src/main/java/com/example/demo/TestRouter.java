package com.example.demo;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TestRouter extends EndpointRouteBuilder {
    @Override
    public void configure() {
        onException(IllegalAccessException.class)
                .routeId("Exceptions")
                .maximumRedeliveries(0)
                .handled(true)
                .removeHeaders("*")
                .to(log("Exceptions").level("WARN")
                            .showBody(false)
                            .showBodyType(false)
                            .showHeaders(true)
                            .multiline(true))
                .to(direct("reply"));

        restConfiguration()
                .bindingMode(RestBindingMode.auto)
                .component("platform-http")
                .inlineRoutes(true)
                .dataFormatProperty("prettyPrint", "true")
                .apiProperty("cors", "true");

        rest("/test")
            .post()
            .id("REST-POST")
            .consumes("application/json")
            .produces("application/json")
            .outType(ResponseMessage.class)
            .to("direct:start");

        from(direct("start"))
                .setBody(constant(List.of("A", "B")))
                .to(direct("line"));
//                .toV(direct("line").getUri(), "mySend", "myReceive");

        from("direct:line")
            .to("log:line")
            .process(new MyProcessor())
            .to("mock:line");

        from(direct("reply"))
                .routeId("createResponse")
                .description("Creates a unified response")
                .process(new JsonResponseProcessor())
//                .marshal().json(JsonLibrary.Jackson, true)
                .to(log("DIRECT_REPLY").showBody(true)
                            .showVariables(true)
                            .showBodyType(true)
                            .showHeaders(true)
                            .multiline(true))
                .end();
    }

    private class MyProcessor implements Processor {
        @Override
        public void process(final Exchange exchange) throws Exception {
            log.info(exchange.getIn()
                             .getBody(String.class));
            throw new IllegalAccessException("Error occurred");
        }
    }

    private class JsonResponseProcessor implements Processor {
        @Override
        public void process(final Exchange exchange) {

            Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
            ResponseMessage message = new ResponseMessage();

            final Message in = exchange.getIn();
            if (cause != null) {
                String responseCode = in.getHeader(Exchange.HTTP_RESPONSE_CODE, String.class);

                String reason = "Unspecific Error";
                String errorString = cause.getMessage();
                int statusCode = 1000;

                in.setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
                message.setError(statusCode,
                                 String.format("ERROR message = %s(%s)", reason, errorString));

            }
            in.setBody(message);
        }
    }
}
