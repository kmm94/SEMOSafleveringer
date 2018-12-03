package karim.seoms.seoms;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import de.siegmar.fastcsv.writer.CsvAppender;
import de.siegmar.fastcsv.writer.CsvWriter;

/**
 * Concurrent Task for assignment 7
 */
public class WriteToCSVFileTask extends AsyncTask<String ,Void,Boolean> {
    private IsDoneWritingToCSV isDoneWritingToCSV;
    private String filename;
    private boolean isCompleted = true;

    public WriteToCSVFileTask(IsDoneWritingToCSV isDoneWritingToCSV) {
        this.isDoneWritingToCSV = isDoneWritingToCSV;
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        filename = strings[0];
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), strings[0]);
            CsvWriter csvWriter = new CsvWriter();
            csvWriter.setFieldSeparator(';');
            try (CsvAppender csvAppender = csvWriter.append(file, StandardCharsets.UTF_8)) {
                //Headers
                csvAppender.appendLine("x", "y", "z");
                //data
                for(int i =1; i <strings.length-3; i+=2)
                {
                    try {
                        String x = strings[i];
                        String y = strings[i+=2];
                        String z = strings[i+=2];

                        csvAppender.appendLine(x,y,z);
                        Log.d(MainActivity.WRITE_TO_CSV_TAG,"Writing Data: "+ strings[0]);
                    } catch (IOException e) {
                        isCompleted = false;
                        e.printStackTrace();
                    }
                }

            } catch (IOException e) {
                isCompleted = false;
                e.printStackTrace();
            }
        }
    return isCompleted;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        isDoneWritingToCSV.isDone(isCompleted,filename);
    }

    @Override
    protected void onPostExecute(Boolean isCompleted) {
        super.onPostExecute(isCompleted);
        isDoneWritingToCSV.isDone(isCompleted, filename);
    }
}
