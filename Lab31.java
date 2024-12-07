import java.util.Random;
import java.util.concurrent.*;

public class Lab31 {
    public static void main(String[] args) {
        try {
            // Введення даних
            int rows = readInput("Введіть кількість рядків: ");
            int cols = readInput("Введіть кількість стовпців: ");
            int minValue = readInput("Введіть мінімальне значення: ");
            int maxValue = readInput("Введіть максимальне значення: ");

            // Генерація масиву
            int[][] array = generateArray(rows, cols, minValue, maxValue);
            System.out.println("Згенерований масив:");
            printArray(array);

            // Work Stealing
            ForkJoinPool forkJoinPool = new ForkJoinPool();
            long startTime = System.nanoTime();
            Integer resultStealing = forkJoinPool.invoke(new WorkStealingTask(array, 0, rows));
            long durationStealing = System.nanoTime() - startTime;
            System.out.println("Work Stealing результат: " + (resultStealing != null ? resultStealing : "Елемент не знайдено"));
            System.out.println("Час виконання (нс): " + durationStealing);

            // Work Dealing
            ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            startTime = System.nanoTime();
            Integer resultDealing = workDealing(array, executor);
            long durationDealing = System.nanoTime() - startTime;
            executor.shutdown();
            System.out.println("Work Dealing результат: " + (resultDealing != null ? resultDealing : "Елемент не знайдено"));
            System.out.println("Час виконання (нс): " + durationDealing);
        } catch (Exception e) {
            System.out.println("Помилка: " + e.getMessage());
        }
    }

    private static int readInput(String message) throws Exception {
        System.out.print(message);
        return new java.util.Scanner(System.in).nextInt();
    }

    private static int[][] generateArray(int rows, int cols, int min, int max) {
        Random random = new Random();
        int[][] array = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                array[i][j] = random.nextInt(max - min + 1) + min;
            }
        }
        return array;
    }

    private static void printArray(int[][] array) {
        for (int[] row : array) {
            for (int value : row) {
                System.out.print(value + "\t");
            }
            System.out.println();
        }
    }

    static class WorkStealingTask extends RecursiveTask<Integer> {
        private final int[][] array;
        private final int startRow;
        private final int endRow;

        WorkStealingTask(int[][] array, int startRow, int endRow) {
            this.array = array;
            this.startRow = startRow;
            this.endRow = endRow;
        }

        @Override
        protected Integer compute() {
            if (endRow - startRow <= 10) { // Малий діапазон для прямого обчислення
                for (int i = startRow; i < endRow; i++) {
                    for (int j = 0; j < array[i].length; j++) {
                        if (array[i][j] == i + j) {
                            return array[i][j];
                        }
                    }
                }
                return null;
            } else {
                int mid = (startRow + endRow) / 2;
                WorkStealingTask task1 = new WorkStealingTask(array, startRow, mid);
                WorkStealingTask task2 = new WorkStealingTask(array, mid, endRow);
                task1.fork();
                Integer result = task2.compute();
                return result != null ? result : task1.join();
            }
        }
    }

    private static Integer workDealing(int[][] array, ExecutorService executor) throws InterruptedException, ExecutionException {
        int rows = array.length;
        int chunkSize = rows / Runtime.getRuntime().availableProcessors();
        Future<Integer>[] futures = new Future[Runtime.getRuntime().availableProcessors()];
        for (int i = 0; i < futures.length; i++) {
            int startRow = i * chunkSize;
            int endRow = (i == futures.length - 1) ? rows : (i + 1) * chunkSize;
            futures[i] = executor.submit(() -> {
                for (int row = startRow; row < endRow; row++) {
                    for (int col = 0; col < array[row].length; col++) {
                        if (array[row][col] == row + col) {
                            return array[row][col];
                        }
                    }
                }
                return null;
            });
        }
        for (Future<Integer> future : futures) {
            Integer result = future.get();
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
