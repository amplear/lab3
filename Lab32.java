import java.io.File;
import java.awt.Desktop;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import javax.swing.*;

public class Lab32 {
    public static void main(String[] args) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) {
            System.out.println("Директорію не вибрано. Завершення програми.");
            return;
        }
        File directory = chooser.getSelectedFile();
        ForkJoinPool pool = new ForkJoinPool();
        ImageSearchTask task = new ImageSearchTask(directory);
        List<File> imageFiles = pool.invoke(task);
        System.out.println("Знайдено зображень: " + imageFiles.size());
        if (!imageFiles.isEmpty()) {
            System.out.println("Відкриваю останній файл: " + imageFiles.get(imageFiles.size() - 1));
            openFile(imageFiles.get(imageFiles.size() - 1));
        } else {
            System.out.println("Жодного зображення не знайдено.");
        }
    }
    private static void openFile(File file) {
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            System.err.println("Не вдалося відкрити файл: " + e.getMessage());
        }
    }
    static class ImageSearchTask extends RecursiveTask<List<File>> {
        private final File directory;

        public ImageSearchTask(File directory) {
            this.directory = directory;
        }

        @Override
        protected List<File> compute() {
            List<File> imageFiles = new ArrayList<>();
            List<ImageSearchTask> subTasks = new ArrayList<>();
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        ImageSearchTask subTask = new ImageSearchTask(file);
                        subTasks.add(subTask);
                        subTask.fork();
                    } else if (isImageFile(file)) {
                        imageFiles.add(file);
                    }
                }
            }
            for (ImageSearchTask subTask : subTasks) {
                imageFiles.addAll(subTask.join());
            }
            return imageFiles;
        }
        private boolean isImageFile(File file) {
            String[] imageExtensions = {".jpg", ".jpeg", ".png", ".bmp", ".gif"};
            String fileName = file.getName().toLowerCase();
            for (String ext : imageExtensions) {
                if (fileName.endsWith(ext)) {
                    return true;
                }
            }
            return false;
        }
    }
}
