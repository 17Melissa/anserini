package io.anserini.index.generator;

import io.anserini.collection.SourceDocument;
import io.anserini.index.Constants;
import org.apache.lucene.document.*;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Converts a {@link SourceDocument} from SafeTensors format into a Lucene {@link Document}, ready to be indexed.
 * Assumes SafeTensors are stored as a binary file of floats.
 *
 * @param <T> type of the source document
 */
public class HnswSafetensorsDenseVectorDocumentGenerator<T extends SourceDocument> implements LuceneDocumentGenerator<T> {

  public HnswSafetensorsDenseVectorDocumentGenerator() {
  }

  private float[] loadVector(String safetensorPath) throws IOException {
    Path path = Paths.get(safetensorPath);
    byte[] data = Files.readAllBytes(path);
    ByteBuffer buffer = ByteBuffer.wrap(data);
    FloatBuffer floatBuffer = buffer.asFloatBuffer();
    float[] vector = new float[floatBuffer.remaining()];
    floatBuffer.get(vector);
    return vector;
  }

  @Override
  public Document createDocument(T src) throws InvalidDocumentException {
    String id = src.id();
    float[] contents;

    try {
      contents = loadVector(src.contents());
    } catch (IOException e) {
      System.err.println("Failed to load dense vector: " + e.getMessage());
      throw new InvalidDocumentException();
    }

    // Make a new, empty document.
    final Document document = new Document();

    // Store the collection docid.
    document.add(new StringField(Constants.ID, id, Field.Store.YES));
    // This is needed to break score ties by docid.
    document.add(new BinaryDocValuesField(Constants.ID, new BytesRef(id)));

    // Add the vector field just like in the original generator
    document.add(new KnnFloatVectorField(Constants.VECTOR, contents, VectorSimilarityFunction.DOT_PRODUCT));

    return document;
  }
}
