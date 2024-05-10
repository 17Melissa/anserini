package io.anserini.collection;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.HashSet;
import java.util.NoSuchElementException;

public class SafeTensorCollection extends DocumentCollection<SafeTensorCollection.Document> {
    public SafeTensorCollection(Path path) {
        this.path = path;
        this.allowedFileSuffix = new HashSet<>();
        this.allowedFileSuffix.add(".safetensors");
        System.out.println("Initialized SafeTensorCollection with path: " + path);
    }

    @Override
    public FileSegment<Document> createFileSegment(Path p) throws IOException {
        System.out.println("Creating file segment for path: " + p);
        return new SafeTensorCollection.Segment(p);
    }

    @Override
    public FileSegment<Document> createFileSegment(BufferedReader bufferedReader) throws IOException {
        System.out.println("Unsupported operation for BufferedReader input.");
        throw new UnsupportedOperationException("Unimplemented method 'createFileSegment(BufferedReader)'");
    }

    public static class Segment extends FileSegment<Document> {
        private ByteBuffer buffer;

        public Segment(Path path) throws IOException {
            super(path);
            buffer = readFile(path);
            buffer.order(ByteOrder.LITTLE_ENDIAN); // Assuming the tensor data is stored in little endian format
            System.out.println("File read into ByteBuffer for path: " + path);
        }

        private ByteBuffer readFile(Path path) throws IOException {
            try (FileInputStream fis = new FileInputStream(path.toFile());
                 FileChannel channel = fis.getChannel()) {
                ByteBuffer buf = ByteBuffer.allocate((int) channel.size());
                channel.read(buf);
                buf.flip();
                System.out.println("Read file into buffer: " + path);
                return buf;
            }
        }

        @Override
        public boolean hasNext() {
            System.out.println("Checking if there's another document in the buffer.");
            return buffer.hasRemaining();
        }

        @Override
        public Document next() {
            if (!hasNext()) {
                System.out.println("No more documents available.");
                throw new NoSuchElementException("No more documents in the file.");
            }
            return parseNextDocument();
        }

        private Document parseNextDocument() {
            System.out.println("Parsing next document.");
            String id = readNextString();
            ByteBuffer content = readTensorData();
            System.out.println("Created document with ID: " + id);
            return new Document(id, content);
        }

        private String readNextString() {
            int length = buffer.getInt();
            byte[] bytes = new byte[length];
            buffer.get(bytes);
            String result = new String(bytes);
            System.out.println("Read string of length " + length + ": " + result);
            return result;
        }

        private ByteBuffer readTensorData() {
            int dtypeLength = buffer.getInt();
            byte[] dtypeBytes = new byte[dtypeLength];
            buffer.get(dtypeBytes);
            String dtype = new String(dtypeBytes);
            System.out.println("Data type of tensor: " + dtype);

            int bytesPerElement = getBytesPerElement(dtype);
            int numDimensions = buffer.getInt();
            System.out.println("Number of dimensions: " + numDimensions);
            int[] shape = new int[numDimensions];
            for (int i = 0; i < numDimensions; i++) {
                shape[i] = buffer.getInt();
                System.out.println("Dimension " + i + ": " + shape[i]);
            }

            int totalElements = 1;
            for (int dim : shape) {
                totalElements *= dim;
            }

            ByteBuffer slice = buffer.slice();
            slice.limit(totalElements * bytesPerElement);
            buffer.position(buffer.position() + totalElements * bytesPerElement);
            System.out.println("Read tensor data with total elements: " + totalElements);
            return slice;
        }

        private int getBytesPerElement(String dtype) {
            int bytes = switch (dtype) {
                case "F32", "I32", "U32" -> 4;
                case "F64", "I64", "U64" -> 8;
                case "F16", "I16", "U16" -> 2;
                case "U8", "I8" -> 1;
                default -> throw new IllegalArgumentException("Unsupported dtype: " + dtype);
            };
            System.out.println("Bytes per element for dtype " + dtype + ": " + bytes);
            return bytes;
        }

        @Override
        protected void readNext() throws IOException, ParseException, NoSuchElementException {
            throw new UnsupportedOperationException("Unimplemented method 'readNext'");
        }
    }

    public static class Document implements SourceDocument {
        private String id;
        private ByteBuffer content;

        public Document(String id, ByteBuffer content) {
            this.id = id;
            this.content = content;
            System.out.println("Document object created with ID: " + id);
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String contents() {
            StringBuilder sb = new StringBuilder();
            content.rewind();
            while (content.hasRemaining()) {
                sb.append(content.getFloat()).append(" "); // Assuming float data for simplicity
            }
            String result = sb.toString().trim();
            System.out.println("Converted ByteBuffer to string content: " + result);
            return result;
        }

        @Override
        public String raw() {
            return contents();  // Simplified to show content directly for demo purposes
        }

        @Override
        public boolean indexable() {
            return true; // Here, you might add logic to decide based on content or metadata
        }
    }
}
