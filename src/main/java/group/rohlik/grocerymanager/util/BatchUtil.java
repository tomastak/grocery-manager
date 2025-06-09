package group.rohlik.grocerymanager.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility class for batch processing.
 *
 * @author Tomas Kramec
 */
public class BatchUtil {

    /**
     * Splits a list into batches of a specified size.
     * This is useful for processing large lists in manageable chunks.
     *
     * @param items     the list to be split
     * @param batchSize the size of each batch
     * @param <T>       the type of elements in the list
     * @return a list of batches, where each batch is a sublist of the original list
     */
    public static <T> List<List<T>> splitIntoBatches(List<T> items, int batchSize) {
        return IntStream.range(0, (items.size() + batchSize - 1) / batchSize)
                .mapToObj(i -> items.subList(i * batchSize, Math.min(items.size(), (i + 1) * batchSize)))
                .collect(Collectors.toList());
    }
}
