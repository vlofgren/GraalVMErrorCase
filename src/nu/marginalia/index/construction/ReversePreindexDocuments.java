package nu.marginalia.index.construction;

import nu.marginalia.array.LongArray;
import nu.marginalia.array.LongArrayFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** A LongArray with document data, segmented according to
 * the associated ReversePreindexWordSegments data
 */
public class ReversePreindexDocuments {
    final Path file;
    public  final LongArray documents;

    public ReversePreindexDocuments(LongArray documents, Path file) {
        this.documents = documents;
        this.file = file;
    }

    public ReversePreindexDocuments(Path file) throws IOException {
        this.file = file;
        this.documents = LongArrayFactory.mmapForReadingConfined(file);
    }

    public FileChannel createDocumentsFileChannel() throws IOException {
        return (FileChannel) Files.newByteChannel(file, StandardOpenOption.READ);
    }



    public long size() {
        return documents.size();
    }

    public void delete() throws IOException {
        Files.delete(this.file);
        documents.close();
    }

}
