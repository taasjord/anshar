package no.rutebanken.anshar.routes.siri.processor;

import no.rutebanken.anshar.routes.siri.processor.routedata.NetexUpdaterService;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.EstimatedVersionFrameStructure;
import uk.org.siri.siri20.Siri;

import java.util.*;
import java.util.stream.Collectors;

import static no.rutebanken.anshar.routes.siri.processor.BaneNorSiriStopAssignmentPopulaterTest.unmarshallSiriFile;
import static org.junit.Assert.*;

@Ignore
public class BaneNorSiriEtRewriterTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void testMapping() throws Exception {
        logger.info("Reads routedata...");
        NetexUpdaterService.update("src/test/resources/rb_nsb-aggregated-netex.zip",
                "src/test/resources/rb_gjb-aggregated-netex.zip",
                "src/test/resources/rb_flt-aggregated-netex.zip",
                "src/test/resources/CurrentAndFuture_latest.zip");
//        NSBGtfsUpdaterService.update("src/test/resources/rb_nsb-aggregated-gtfs.zip",
//                "src/test/resources/rb_gjb-aggregated-gtfs.zip",
//                "src/test/resources/rb_flt-aggregated-gtfs.zip");

        BaneNorSiriEtRewriter rewriter = new BaneNorSiriEtRewriter();

        Siri siri = unmarshallSiriFile("src/test/resources/siri-et-gir-npe.xml");
//        Siri siri = unmarshallSiriFile("src/test/resources/siri-et-from-bnr.xml");
        HashMap<String, List<String>> trainNumbersToStopsBefore = mapTrainNumbersToStops(siri);

        rewriter.process(siri);
        HashMap<String, List<String>> trainNumbersToStopsAfter = mapTrainNumbersToStops(siri);

        assertEquals(trainNumbersToStopsBefore.size(), trainNumbersToStopsAfter.size());
        for (Map.Entry<String, List<String>> before : trainNumbersToStopsBefore.entrySet()) {

            List<String> afterStops = trainNumbersToStopsAfter.get(before.getKey());
            List<String> beforeStops = before.getValue();
            if (afterStops.size() != beforeStops.size()) {
                logger.error("Trainnumber {} now has {} stops and not {} as before", before.getKey(), afterStops.size(), beforeStops.size());
            } else if (!afterStops.containsAll(beforeStops)){
                logger.error("The stops differ before and after:" +
                        "\n  Before: {}" +
                        "\n  After : {}", beforeStops, afterStops);
            }
        }

    }

    private HashMap<String, List<String>> mapTrainNumbersToStops(Siri siri) {
        HashMap<String, List<String>> result = new HashMap<>();
        List<EstimatedTimetableDeliveryStructure> etDeliveries = siri.getServiceDelivery().getEstimatedTimetableDeliveries();
        if (etDeliveries != null) {
            for (EstimatedTimetableDeliveryStructure etDelivery : etDeliveries) {
                List<EstimatedVersionFrameStructure> estimatedJourneyVersionFrames = etDelivery.getEstimatedJourneyVersionFrames();
                for (EstimatedVersionFrameStructure estimatedJourneyVersionFrame : estimatedJourneyVersionFrames) {
                    List<EstimatedVehicleJourney> estimatedVehicleJourneies = estimatedJourneyVersionFrame.getEstimatedVehicleJourneies();
                    for (EstimatedVehicleJourney journey : estimatedVehicleJourneies) {
                        String trainNumber = journey.getVehicleRef().getValue();
                        List<String> recorded = journey.getRecordedCalls() == null ? Collections.emptyList() : journey.getRecordedCalls().getRecordedCalls().stream().map(c -> c.getStopPointRef().getValue()).collect(Collectors.toList());
                        List<String> estimated = journey.getEstimatedCalls() == null ? Collections.emptyList() : journey.getEstimatedCalls().getEstimatedCalls().stream().map(c -> c.getStopPointRef().getValue()).collect(Collectors.toList());
                        ArrayList<String> stops = new ArrayList<>();
                        stops.addAll(recorded);
                        stops.addAll(estimated);
                        result.put(trainNumber, stops);
                    }
                }
            }
        }
        return result;
    }
}