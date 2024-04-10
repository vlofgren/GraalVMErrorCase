import nu.marginalia.index.construction.ReversePreindex;
import nu.marginalia.index.construction.ReversePreindexDocuments;
import nu.marginalia.index.construction.ReversePreindexWordSegments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws InterruptedException, IOException {

        Path basePath = Path.of(args[0]);
        Path testBaseDir = Path.of(args[1]);
        int threadCount = Integer.parseInt(args[2]);
        int iters = Integer.parseInt(args[3]);

        ReversePreindex left = new ReversePreindex(
                new ReversePreindexWordSegments(
                        basePath.resolve("errdump-ldsw14956112138973407107.dat"),
                        basePath.resolve("errdump-ldsc15894180912189163885.dat")
                ),
                new ReversePreindexDocuments(
                        basePath.resolve("errdump-ldf3344245952628310196.dat")
                )
        );

        ReversePreindex right = new ReversePreindex(
                new ReversePreindexWordSegments(
                        basePath.resolve("errdump-rdsw9077474693562582214.dat"),
                        basePath.resolve("errdump-rdsc15090814846662501101.dat")
                ),
                new ReversePreindexDocuments(
                        basePath.resolve("errdump-rdf7546082283657694744.dat")
                )
        );

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            int threadId = i;
            threads.add(
                    Thread.ofPlatform().start(() -> {
                        try {
                            run(testBaseDir, threadId, iters, left, right);
                        }
                        catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    })
            );
        }
        for (var thread : threads) {
            thread.join();
        }

    }

    static void run(Path testBaseDir, int threadId, int iters, ReversePreindex left, ReversePreindex right) throws IOException {
        Path tempDir = Files.createTempDirectory(testBaseDir, "thrasher");

        for (int i = 0; i < iters; i++) {
            var ret = ReversePreindex.merge(tempDir, left, right);
            ret.delete();

            List<Path> contents = new ArrayList<>();
            Files.list(tempDir).forEach(contents::add);
            for (var tempFile : contents) {
                Files.delete(tempFile);
            }

            System.out.printf("Thread[%d] iter %d done\n", threadId, i);
        }
    }

}