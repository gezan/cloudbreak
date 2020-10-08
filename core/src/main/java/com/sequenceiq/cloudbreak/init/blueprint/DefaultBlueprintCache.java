package com.sequenceiq.cloudbreak.init.blueprint;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.cdp.shaded.com.google.common.collect.Sets;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.converter.v4.blueprint.BlueprintV4RequestToBlueprintConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Service
public class DefaultBlueprintCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBlueprintCache.class);

    private final Map<String, Blueprint> defaultBlueprints = new HashMap<>();

    @Inject
    private BlueprintEntities blueprintEntities;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private BlueprintV4RequestToBlueprintConverter converter;

    @PostConstruct
    public void loadBlueprintsFromFile() {
        Map<String, Set<String>> blueprints = blueprints();
        for (Map.Entry<String, Set<String>> blueprintEntry : blueprints.entrySet()) {
            try {
                for (String blueprintText : blueprintEntry.getValue()) {
                    String[] split = blueprintText.trim().split("=");
                    if (blueprintUtils.isBlueprintNamePreConfigured(blueprintText, split)) {
                        LOGGER.debug("Load default validation '{}'.", AnonymizerUtil.anonymize(blueprintText));
                        BlueprintV4Request blueprintJson = new BlueprintV4Request();
                        blueprintJson.setName(split[0].trim());
                        JsonNode jsonNode = blueprintUtils.convertStringToJsonNode(
                                blueprintUtils.readDefaultBlueprintFromFile(blueprintEntry.getKey(), split));
                        blueprintJson.setBlueprint(jsonNode.get("blueprint").toString());
                        Blueprint bp = converter.convert(blueprintJson);
                        JsonNode tags = jsonNode.get("tags");
                        Map<String, Object> tagParameters = blueprintUtils.prepareTags(tags);
                        bp.setTags(new Json(tagParameters));
                        JsonNode description = jsonNode.get("description");
                        bp.setDescription(description == null ? split[0] : description.asText(split[0]));
                        defaultBlueprints.put(bp.getName(), bp);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Can not read default validation from file: ", e);
            }
        }
    }

    public Map<String, Blueprint> defaultBlueprints() {
        Map<String, Blueprint> result = new HashMap<>();
        defaultBlueprints.forEach((key, value) -> result.put(key, SerializationUtils.clone(value)));
        return result;
    }

    private Map<String, Set<String>> blueprints() {
        return blueprintEntities.getDefaults()
                .entrySet()
                .stream()
                .filter(e -> StringUtils.isNoneBlank(e.getValue()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> Sets.newHashSet(e.getValue().split(";"))));
    }
}
