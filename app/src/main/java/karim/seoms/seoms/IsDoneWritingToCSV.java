package karim.seoms.seoms;

public interface IsDoneWritingToCSV {
    /**
     * Is a callback method for the AsyncTask
     * @param isCompleted boolean if done it is true if some error happens is false.
     * @param filename The name of the file
     */
    void isDone(Boolean isCompleted, String filename);
}
