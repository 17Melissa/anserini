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
    }

    @Override
    public FileSegment<Document> createFileSegment(Path p) throws IOException {
        return new SafeTensorCollection.Segment(p);
    }

    @Override
    public FileSegment<Document> createFileSegment(BufferedReader bufferedReader) throws IOException {
       return new SafeTensorCollection.Segment(bufferedReader);
    }

    public static class Segment extends FileSegment<Document> {
        private ByteBuffer buffer;

        public Segment(Path path) throws IOException {
            super(path);
            buffer = readFile(path);
            buffer.order(ByteOrder.LITTLE_ENDIAN); // Assuming the tensor data is stored in little endian format
        }
        public Segment(BufferedReader bufferedReader) throws IOException {
            super(bufferedReader);
            throw new UnsupportedOperationException("Unimplemented method 'Segment(BufferedReader bufferedReader)'");
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

        private Document parseNextDocument() {
            String id = readNextString();
            ByteBuffer content = readTensorData();
            return new Document(id, content);
        }

        private String readNextString() {
            int length = buffer.getInt();
            byte[] bytes = new byte[length];
            buffer.get(bytes);
            return new String(bytes);
        }

        private ByteBuffer readTensorData() {
            // Read metadata or header to understand tensor structure
            int dtypeLength = buffer.getInt();
            byte[] dtypeBytes = new byte[dtypeLength];
            buffer.get(dtypeBytes);
            String dtype = new String(dtypeBytes);
        
            // Determine the size in bytes per element based on dtype
            int bytesPerElement = getBytesPerElement(dtype);
        
            int numDimensions = buffer.getInt(); // Reading number of dimensions
            int[] shape = new int[numDimensions];
            for (int i = 0; i < numDimensions; i++) {
                shape[i] = buffer.getInt(); // Reading each dimension
            }
        
            // Calculate total number of elements
            int totalElements = 1;
            for (int dim : shape) {
                totalElements *= dim;
            }
        
            ByteBuffer slice = buffer.slice();
            slice.limit(totalElements * bytesPerElement);
            buffer.position(buffer.position() + totalElements * bytesPerElement);
            return slice;
        }
        
        private int getBytesPerElement(String dtype) {
            switch (dtype) {
                case "F32": case "I32": case "U32":
                    return 4;
                case "F64": case "I64": case "U64":
                    return 8;
                case "F16": case "I16": case "U16":
                    return 2;
                case "U8": case "I8":
                    return 1;
                default:
                    throw new IllegalArgumentException("Unsupported dtype: " + dtype);
            }
        }
        

        @Override
        protected void readNext() throws IOException, ParseException, NoSuchElementException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'readNext'");
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
            // Convert content according to actual dtype and intended usage
            StringBuilder sb = new StringBuilder();
            content.rewind();
            while (content.hasRemaining()) {
                sb.append(content.getFloat()).append(" "); // Assuming float data for simplicity
            }
            return sb.toString().trim();
        }

        @Override
        public String raw() {
            // Return raw binary data as a hexadecimal string
            return contents();  // Simplified to show content directly for demo purposes
        }

        @Override
        public boolean indexable() {
            return true; // Here, you might add logic to decide based on content or metadata
        }
    }


}
