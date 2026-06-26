package it.unive.jlisa.springed.p1.output;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import it.unive.jlisa.springed.p1.constructs.Mapping;
import it.unive.jlisa.springed.p1.constructs.Registry;
import java.io.IOException;

public class RegistryJsonSerializer extends JsonSerializer<Registry> {

	@Override
	public void serialize(
			Registry registry,
			JsonGenerator gen,
			SerializerProvider serializers)
			throws IOException {
		gen.writeStartObject();

		for (Mapping mapping : registry.getMappings()) {
			gen.writeFieldName(mapping.getJsonFieldName());
			serializers.defaultSerializeValue(mapping, gen);
		}

		gen.writeEndObject();
	}
}
