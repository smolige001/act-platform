package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class NamespaceTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testEncodeNamespace() {
    Namespace namespace = Namespace.builder()
            .setId(UUID.randomUUID())
            .setName("namespace")
            .build();

    JsonNode root = mapper.valueToTree(namespace);
    assertEquals(namespace.getId().toString(), root.get("id").textValue());
    assertEquals(namespace.getName(), root.get("name").textValue());
  }

}
