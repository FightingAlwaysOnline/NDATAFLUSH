import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.*;

public class FlushFile {


    private int CopyFile(String originFile, String duplicateFile) {
        File file = new File(originFile);
        File file1 = new File(duplicateFile);

        file1.deleteOnExit();
        try {
            file1.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileInputStream in = new FileInputStream(file);
            FileOutputStream out = new FileOutputStream(file1);
            BufferedInputStream inputStream = new BufferedInputStream(in);
            BufferedOutputStream outputStream = new BufferedOutputStream(out);
            int indata;
            int outdata;
            while ((indata = inputStream.read()) >= 0) {
                outputStream.write((char) indata);
            }

            outputStream.flush();
            inputStream.close();
            outputStream.close();
            in.close();
            out.close();
            return 1;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int CreateWordFile(String FilePath,String fileName,String content){
        XWPFDocument document=new XWPFDocument();
        XWPFParagraph paragraph=document.createParagraph();
        String[] con=content.split("\\n");

        for(int i=0;i<con.length;i++){
            XWPFRun run=paragraph.createRun();
            run.addBreak();
            run.addBreak();
            run.setText(con[i]);
        }

        File file =new File(FilePath+"\\"+fileName);

            try {

                file.createNewFile();
                FileOutputStream out=new FileOutputStream(file);
                document.write(out);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            }
        return 1;
    }

    public int CreateImg(String Filepath,String docPath,String Filename,BufferedInputStream in) {



        File file=new File(Filepath+"//"+docPath+"//"+Filename);

        try {

            FileOutputStream out=new FileOutputStream(file);
            BufferedOutputStream writer=new BufferedOutputStream(out);
            int Temp_data=0;
            while((Temp_data=in.read())!=-1){
                writer.write(Temp_data);
            }
            writer.flush();
            writer.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }


        return 1;
    }
}
