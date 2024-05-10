

package io.anserini.collection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ByteArrayInputStream;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MappingIterator;

public class SafeTensorCollection extends DocumentCollection<SafeTensorCollection.Document> {
    private static final Logger LOG = LogManager.getLogger(SafeTensorCollection.class);

    public SafeTensorCollection() {
    }

    public SafeTensorCollection(Path path) {
        this.path = path;
        this.allowedFileSuffix = new HashSet<>(Arrays.asList(".safetensor"));
    }

    @Override
    public FileSegment<Document> createFileSegment(Path p) throws IOException {
        return new Segment(p);
    }

    @Override
    public FileSegment<Document> createFileSegment(BufferedReader bufferedReader) throws IOException {
        return new Segment(bufferedReader);
    }

    public static class Segment extends FileSegment<Document> {
        private ObjectMapper mapper = new ObjectMapper();
        private JsonNode node = null;
        private Iterator<JsonNode> iter = null; // iterator for JSON document array
        private MappingIterator<JsonNode> iterator; // iterator for JSON line objects

        public Segment(Path path) throws IOException {
            super(path);

            // Assuming the tensor data is serialized in JSON format for simplicity
            bufferedReader = Files.newBufferedReader(path);
            iterator = mapper.readerFor(JsonNode.class).readValues(bufferedReader);
            if (iterator.hasNext()) {
                node = iterator.next();
                if (node.isArray()) {
                    iter = node.elements();
                }
            }
        }

        public Segment(BufferedReader bufferedReader) throws IOException {
            super(bufferedReader);
            iterator = mapper.readerFor(JsonNode.class).readValues(bufferedReader);
            if (iterator.hasNext()) {
                node = iterator.next();
                if (node.isArray()) {
                    iter = node.elements();
                }
            }
        }

        @Override
        public void readNext() throws NoSuchElementException {
            if (node == null) {
                throw new NoSuchElementException("JsonNode is empty");
            } else if (node.isObject()) {
                bufferedRecord = createNewDocument(node);
                if (iterator.hasNext()) {
                    node = iterator.next();
                } else {
                    atEOF = true;
                }
            } else {
                LOG.error("Error: invalid JsonNode type");
                throw new NoSuchElementException("Invalid JsonNode type");
            }
        }

        protected Document createNewDocument(JsonNode json) {
            return new Document(json);
        }
    }

    public static class Document extends MultifieldSourceDocument {
        private static final ObjectMapper MAPPER = new ObjectMapper();

        private String id;
        private String contents;
        private String raw;
        private Map<String, String> fields;

        public static Document fromString(String raw) throws IOException {
            MappingIterator<JsonNode> iterator = MAPPER.readerFor(JsonNode.class).readValues(new ByteArrayInputStream(raw.getBytes()));
            if (iterator.hasNext()) {
                return new Document(iterator.next());
            }

            return null;
        }

        public static Document fromFields(String id, String contents) throws IOException {
            return new Document(MAPPER.createObjectNode().put("id", id).put("contents", contents));
        }

        public Document(JsonNode json) {
            this.raw = json.toPrettyString();
            this.fields = new HashMap<>();

            json.fields().forEachRemaining(entry -> {
                if ("id".equals(entry.getKey())) {
                    this.id = json.get("id").asText();
                } else if ("contents".equals(entry.getKey())) {
                    this.contents = json.get("contents").asText();
                } else {
                    this.fields.put(entry.getKey(), entry.getValue().asText());
                }
            });
        }

        @Override
        public String id() {
            if (id == null) {
                throw new RuntimeException("JSON document has no \"id\" field!");
            }
            return id;
        }

        @Override
        public String contents() {
            if (contents == null) {
                throw new RuntimeException("JSON document has no \"contents\" field!");
            }
            return contents;
        }

        @Override
        public String raw() {
            return raw;
        }

        @Override
        public boolean indexable() {
            return true;
        }

        @Override
        public Map<String, String> fields() {
            return fields;
        }
    }
}
