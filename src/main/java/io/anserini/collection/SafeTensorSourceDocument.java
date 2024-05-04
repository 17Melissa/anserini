package io.anserini.collection;

import java.nio.ByteBuffer;

public class SafeTensorSourceDocument implements SourceDocument {
    private String documentId;
    private ByteBuffer content;

    public SafeTensorSourceDocument(String documentId, ByteBuffer content) {
        this.documentId = documentId;
        this.content = content;
    }

    @Override
    public String id() {
        return documentId;
    }

    @Override
    public String contents() {
        content.rewind();
        StringBuilder sb = new StringBuilder();
        while (content.hasRemaining()) {
            sb.append(content.getFloat()).append(" "); // Assuming the content is entirely of float type
        }
        return sb.toString().trim();
    }

    @Override
    public String raw() {
        return contents(); // Alternatively, return a more detailed binary or structured representation
    }

    @Override
    public boolean indexable() {
        return true; // Add logic if some documents should not be indexed
    }

    // Additional method to access the raw byte buffer if needed
    public ByteBuffer getContentBuffer() {
        return content;
    }
}
