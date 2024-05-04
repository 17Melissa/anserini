package io.anserini.collection;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.NoSuchElementException;

public class SafeTensorCollection extends DocumentCollection<SafeTensorCollection.Document> {
    public SafeTensorCollection(Path path) {
        this.path = path;
        this.allowedFileSuffix = new HashSet<>();
        this.allowedFileSuffix.add(".safetensors");
    }

    public SafeTensorCollection() {
    }

    @Override
    public FileSegment<SafeTensorCollection.Document> createFileSegment(Path p) throws IOException {
        return new SafeTensorCollection.Segment(p);
    }

    public static class Segment extends FileSegment<Document> {
        private ByteBuffer buffer;

        public Segment(Path path) throws IOException {
            super(path);
            buffer = readFile(path);
        }

        private ByteBuffer readFile(Path path) throws IOException {
            try (FileInputStream fis = new FileInputStream(path.toFile());
                 FileChannel channel = fis.getChannel()) {
                ByteBuffer buf = ByteBuffer.allocate((int) channel.size());
                channel.read(buf);
                buf.flip();
                return buf;
            }
        }

        
        public boolean hasNext() {
            return buffer.hasRemaining();
        }

        
        public Document next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more documents in the file.");
            }
            return parseNextDocument();
        }

        @Override
        public void readNext() {
            parseNextDocument();
        }

        private Document parseNextDocument() {
            String id = readNextString();
            ByteBuffer content = readNextContent();
            return new Document(id, content);
        }

        private String readNextString() {
            int length = buffer.getInt();
            byte[] bytes = new byte[length];
            buffer.get(bytes);
            return new String(bytes);
        }

        private ByteBuffer readNextContent() {
            int size = buffer.getInt();
            ByteBuffer slice = buffer.slice();
            slice.limit(size);
            buffer.position(buffer.position() + size);
            return slice;
        }
    }

    public static class Document implements SourceDocument {
        private String id;
        private ByteBuffer content;

        public Document(String id, ByteBuffer content) {
            this.id = id;
            this.content = content;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String contents() {
            // Assuming content is a float tensor, convert it to a readable string
            content.rewind();
            StringBuilder sb = new StringBuilder();
            while (content.hasRemaining()) {
                sb.append(content.getFloat()).append(" ");
            }
            return sb.toString().trim();
        }

        @Override
        public String raw() {
            // Return the raw binary data as a hexadecimal string for raw method
            content.rewind();
            StringBuilder sb = new StringBuilder();
            while (content.hasRemaining()) {
                sb.append(String.format("%02x ", content.get()));
            }
            return sb.toString().trim();
        }

        @Override
        public boolean indexable() {
            if(content.hasArray()) {
                return content.array().length > 0;
            }
            return false;
        }
    }

    @Override
    public FileSegment<Document> createFileSegment(BufferedReader bufferedReader) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createFileSegment'");
    }
}
