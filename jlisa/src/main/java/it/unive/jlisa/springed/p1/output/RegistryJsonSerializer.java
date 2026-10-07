package it.unive.jlisa.springed.p1.output;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import it.unive.jlisa.springed.p1.RegistryRecord;
import it.unive.jlisa.springed.p1.Registry;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class RegistryJsonSerializer extends JsonSerializer<Registry> {

	@Override
	public void serialize(
			Registry registry,
			JsonGenerator gen,
			SerializerProvider serializers)
			throws IOException {
		gen.writeStartObject();

		Map<String, Long> fieldNameCounts = registry.getMappings().stream()
				.collect(Collectors.groupingBy(RegistryRecord::getJsonFieldName, Collectors.counting()));

		for (RegistryRecord mapping : registry.getMappings()) {
			boolean ambiguous = fieldNameCounts.get(mapping.getJsonFieldName()) > 1;
			gen.writeFieldName(ambiguous ? mapping.getJsonFieldNameWithParameters() : mapping.getJsonFieldName());
			serializers.defaultSerializeValue(mapping, gen);
		}

		gen.writeEndObject();
	}
}
