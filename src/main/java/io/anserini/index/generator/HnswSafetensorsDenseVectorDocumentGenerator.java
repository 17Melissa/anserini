package io.anserini.index.generator;

import io.anserini.collection.SourceDocument;
import io.anserini.index.Constants;
import org.apache.lucene.document.BinaryDocValuesField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.util.BytesRef;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Converts a {@link SourceDocument} into a Lucene {@link Document}, ready to be indexed,
 * using dense vector data retrieved from SafeTensor files.
 *
 * @param <T> type of the source document
 */
public class HnswSafetensorsDenseVectorDocumentGenerator<T extends SourceDocument> implements LuceneDocumentGenerator<T> {

  public HnswSafetensorsDenseVectorDocumentGenerator() {
  }

  private float[] convertByteBufferToFloatArray(ByteBuffer byteBuffer) {
    FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
    float[] vector = new float[floatBuffer.remaining()];
    floatBuffer.get(vector);
    return vector;
  }

  @Override
  public Document createDocument(T src) throws InvalidDocumentException {
    String id = src.id();
    float[] contents;

    try {
      // Assuming the content ByteBuffer is properly set and represents a tensor
      ByteBuffer contentBuffer = ByteBuffer.wrap(src.raw().getBytes()); // Needs to be adjusted based on actual usage
      contentBuffer.rewind(); // Ensure the buffer is ready to be read
      contents = convertByteBufferToFloatArray(contentBuffer);
    } catch (Exception e) {
      throw new InvalidDocumentException();
    }

    // Make a new, empty document.
    final Document document = new Document();

    // Store the collection docid.
    document.add(new StringField(Constants.ID, id, Field.Store.YES));
    // This is needed to break score ties by docid.
    document.add(new BinaryDocValuesField(Constants.ID, new BytesRef(id)));

    document.add(new KnnFloatVectorField(Constants.VECTOR, contents, VectorSimilarityFunction.DOT_PRODUCT));

    return document;
  }
}
