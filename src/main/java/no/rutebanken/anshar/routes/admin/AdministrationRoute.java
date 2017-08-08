package no.rutebanken.anshar.routes.admin;

import no.rutebanken.anshar.messages.EstimatedTimetables;
import no.rutebanken.anshar.messages.ProductionTimetables;
import no.rutebanken.anshar.messages.Situations;
import no.rutebanken.anshar.messages.VehicleActivities;
import no.rutebanken.anshar.messages.collections.ExtendedHazelcastService;
import no.rutebanken.anshar.routes.outbound.ServerSubscriptionManager;
import no.rutebanken.anshar.subscription.SubscriptionManager;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
@Configuration
public class AdministrationRoute extends RouteBuilder {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${anshar.incoming.port}")
    private String inboundPort;

    @Autowired
    ExtendedHazelcastService extendedHazelcastService;

    @Autowired
    private SubscriptionManager subscriptionManager;

    @Autowired
    private ServerSubscriptionManager serverSubscriptionManager;


    @Autowired
    private Situations situations;

    @Autowired
    private VehicleActivities vehicleActivities;

    @Autowired
    private EstimatedTimetables estimatedTimetables;

    @Autowired
    private ProductionTimetables productionTimetables;

    @Override
    public void configure() throws Exception {

        //Return subscription status
        from("jetty:http://0.0.0.0:" + inboundPort + "/anshar/stats")
                .process(p-> {
                    p.getOut().setHeader(Exchange.CONTENT_TYPE, "text/html");
                    p.getOut().setBody(subscriptionManager.buildStats());
                })
                .to("freemarker:templates/stats.ftl")
                .routeId("admin.stats")
        ;

        //Stop subscription
        from("jetty:http://0.0.0.0:" + inboundPort + "/anshar/stop?httpMethodRestrict=PUT")
                .process(p -> {
                    HttpServletRequest request = p.getIn().getBody(HttpServletRequest.class);
                    String subscriptionId = request.getParameter("subscriptionId");
                    if (subscriptionId != null &&
                            !subscriptionId.isEmpty()) {
                        subscriptionManager.stopSubscription(subscriptionId);
                    }

                })
                .routeId("admin.stop")
        ;
        //Start subscription
        from("jetty:http://0.0.0.0:" + inboundPort + "/anshar/start?httpMethodRestrict=PUT")
                .process(p -> {
                    HttpServletRequest request = p.getIn().getBody(HttpServletRequest.class);
                    String subscriptionId = request.getParameter("subscriptionId");
                    if (subscriptionId != null &&
                            !subscriptionId.isEmpty()) {

                       subscriptionManager.startSubscription(subscriptionId);
                    }
                })
                .routeId("admin.start")
        ;

        //Return subscription status
        from("jetty:http://0.0.0.0:" + inboundPort + "/anshar/subscriptions")
                .process(p -> {
                    p.getOut().setHeader(Exchange.CONTENT_TYPE, "text/html");
                    p.getOut().setBody(serverSubscriptionManager.getSubscriptionsAsJson());
                })
                .to("freemarker:templates/subscriptions.ftl")
                .routeId("admin.subscriptions")
        ;

        //Return subscription status
        from("jetty:http://0.0.0.0:" + inboundPort + "/anshar/clusterstats")
                .process(p -> {
                    HttpServletRequest request = p.getIn().getBody(HttpServletRequest.class);
                    String includeStats = request.getParameter("stats");
                    p.getOut().setHeader(Exchange.CONTENT_TYPE, "application/json");
                    p.getOut().setBody(extendedHazelcastService.listNodes((includeStats != null && Boolean.valueOf(includeStats))));
                })
                .routeId("admin.clusterstats")
        ;

    }
}
