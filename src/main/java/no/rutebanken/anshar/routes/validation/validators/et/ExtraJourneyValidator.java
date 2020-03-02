/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package no.rutebanken.anshar.routes.validation.validators.et;

import com.google.common.collect.Sets;
import no.rutebanken.anshar.routes.validation.validators.CustomValidator;
import no.rutebanken.anshar.routes.validation.validators.Validator;
import no.rutebanken.anshar.subscription.SiriDataType;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;
import uk.org.siri.siri20.VehicleModesEnumeration;

import javax.xml.bind.ValidationEvent;
import java.util.Set;

import static no.rutebanken.anshar.routes.validation.validators.Constants.ESTIMATED_VEHICLE_JOURNEY;

/**
 * Verifies required values when ExtraJourney set to <code>true</code>
 *  - VehicleMode must be set to a valid value
 *  - RouteRef must be set, and build up correctly
 *  - GroupOfLinesRef must be set, and build up correctly
 *  - EstimatedVehicleJourneyCode must be set, and build up correctly
 *
 */
@Validator(profileName = "norway", targetType = SiriDataType.ESTIMATED_TIMETABLE)
@Component
public class ExtraJourneyValidator extends CustomValidator {

    private static final String FIELDNAME = "ExtraJourney";
    private String path = ESTIMATED_VEHICLE_JOURNEY + FIELD_DELIMITER + FIELDNAME;


    private static final Set<String> validVehicleModes = Sets.newHashSet(
            VehicleModesEnumeration.AIR.value(),
            VehicleModesEnumeration.BUS.value(),
            VehicleModesEnumeration.COACH.value(),
            VehicleModesEnumeration.FERRY.value(),
            VehicleModesEnumeration.METRO.value(),
            VehicleModesEnumeration.RAIL.value(),
            VehicleModesEnumeration.TRAM.value());

    private static final String VEHICLE_MODE_NODE_NAME = "VehicleMode";
    private static final String ROUTE_REF_NODE_NAME = "RouteRef";
    private static final String GROUP_OF_LINES_REF_NODE_NAME = "GroupOfLinesRef";
    private static final String ESTIMATED_VEHICLE_JOURNEY_CODE_NODE_NAME = "EstimatedVehicleJourneyCode";

    @Override
    public String getXpath() {
        return path;
    }

    @Override
    public ValidationEvent isValid(Node node) {
        String isExtraJourney = getNodeValue(node);

        if (isExtraJourney == null) {
            return  createEvent(node, VEHICLE_MODE_NODE_NAME, "not null when present", FIELDNAME, ValidationEvent.ERROR);
        }

        if (Boolean.TRUE.equals(Boolean.valueOf(isExtraJourney))) {
            // ExtraJourney == true

            String expectedValuesMessageText = "not null when ExtraJourney=true";

            // VehicleMode - required
            final String vehicleMode = getSiblingNodeValue(node, VEHICLE_MODE_NODE_NAME);
            if (vehicleMode == null) {
                return  createEvent(node, VEHICLE_MODE_NODE_NAME, expectedValuesMessageText, vehicleMode, ValidationEvent.ERROR);
            } else if (!validVehicleModes.contains(vehicleMode)) {
                return  createEvent(node, VEHICLE_MODE_NODE_NAME, validVehicleModes, vehicleMode, ValidationEvent.ERROR);
            }


            // RouteRef - required
            final String routeRef = getSiblingNodeValue(node, ROUTE_REF_NODE_NAME);
            if (routeRef == null) {
                return  createEvent(node, ROUTE_REF_NODE_NAME, expectedValuesMessageText, routeRef, ValidationEvent.ERROR);
            } else if (!routeRef.contains(":Route:")) {
                return  createEvent(node, ROUTE_REF_NODE_NAME, "valid RouteRef - CODESPACE:Route:ID", routeRef, ValidationEvent.ERROR);
            }

            // GroupOfLinesRef - required
            final String groupOfLines = getSiblingNodeValue(node, GROUP_OF_LINES_REF_NODE_NAME);
            if (groupOfLines == null) {
                return  createEvent(node, GROUP_OF_LINES_REF_NODE_NAME, expectedValuesMessageText, groupOfLines, ValidationEvent.ERROR);
            } else if (!groupOfLines.contains(":Network:")) {
                return  createEvent(node, GROUP_OF_LINES_REF_NODE_NAME, "valid GroupOfLinesRef - CODESPACE:Network:ID", groupOfLines, ValidationEvent.ERROR);
            }

            // EstimatedVehicleJourneyCode - required
            final String estimatedVehicleJourneyCode = getSiblingNodeValue(node, ESTIMATED_VEHICLE_JOURNEY_CODE_NODE_NAME);
            if (estimatedVehicleJourneyCode == null) {
                return  createEvent(node, ESTIMATED_VEHICLE_JOURNEY_CODE_NODE_NAME, expectedValuesMessageText, groupOfLines, ValidationEvent.ERROR);
            } else if (!estimatedVehicleJourneyCode.contains(":ServiceJourney:")) {
                return  createEvent(node, estimatedVehicleJourneyCode, "valid EstimatedVehicleJourneyCode - CODESPACE:ServiceJourney:ID", estimatedVehicleJourneyCode, ValidationEvent.ERROR);
            }
        }


        return null;
    }
}
