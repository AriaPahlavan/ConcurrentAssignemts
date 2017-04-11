import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import javax.naming.Context;
import javax.xml.soap.Text;
import java.util.Arrays;

// Do not change the signature of this class
public class TextAnalyzer extends Configured implements Tool {

    // Replace "?" with your own output key / value types
    // The four template data types are:
    //     <Input Key Type, Input Value Type, Output Key Type, Output Value Type>
    public static class TextMapper extends Mapper<LongWritable, Text, Text, LongWritable> {
        private Text context_query = new Text();
        private final static LongWritable oneOccurence = new LongWritable(1);

        public void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException
        {
            String s = value.toString();
            s = s.toLowerCase();
            s = s.replaceAll("[^a-zA-Z0-9]", " ");
            // Implementation of you mapper function
            String[] split = value.split(" "); //do i need to ignore "."
            for(int i=0; i<split.length; i++){
                String contextword = split[i];
                for(int q=0; q<split.length; q++){
                    if(q == i){
                        //skip over self
                    }
                    else if(split[q] == ""){
                        //skip over empty string
                    }
                    else{
                        String queryword = split[q];
                        String conque = contextword + queryword;
                        context_query.set(conque);
                        context.write(context_query,oneOccurence);
                    }
                }
            }

        }
    }

    // Replace "?" with your own key / value types
    // NOTE: combiner's output key / value types have to be the same as those of mapper
    public static class TextCombiner extends Reducer<Text, LongWritable, Text, LongWritable> {

        private LongWritable querySum = new LongWritable();

        public void reduce(Text key, Iterable<Tuple> tuples, Context context)
                throws IOException, InterruptedException
        {
            // Implementation of you combiner function
            long sum = 0;
            for(LongWritable tup : tuples) {
                sum += tup.get();
            }

            querySum.set(sum);
            context.write(key, querySum);
        }
    }

    // Replace "?" with your own input key / value types, i.e., the output
    // key / value types of your mapper function
    public static class TextReducer extends Reducer<?, ?, Text, Text> {
        private final static Text emptyText = new Text("");
        private Text queryWordText = new Text();

        public void reduce(Text key, Iterable<Tuple> queryTuples, Context context)
                throws IOException, InterruptedException
        {
            // Implementation of you reducer function
            String conqueKey[] = key.split(" ");
            String contextWord = conqueKey[0];

            //   Write out the current context key
            context.write(contextWord, emptyText);
            //   Write out query words and their count
            for(String conqueMap: map.keySet()){
                String[] conque = conqueMap.split(" ");
                if(conque[0] == contextWord) {
                    String count = map.get(conqueMap).toString() + ">";
                    queryWordText.set("<" + conque[1] + ",");
                    context.write(queryWordText, new Text(count));
                }
            }
            //   Empty line for ending the current context key
            context.write(emptyText, emptyText);
        }
    }

    public int run(String[] args) throws Exception {
        Configuration conf = this.getConf();

        // Create job
        Job job = new Job(conf, "EID1_EID2"); // Replace with your EIDs
        job.setJarByClass(TextAnalyzer.class);

        // Setup MapReduce job
        job.setMapperClass(TextMapper.class);
        //   Uncomment the following line if you want to use Combiner class
        // job.setCombinerClass(TextCombiner.class);
        job.setReducerClass(TextReducer.class);

        // Specify key / value types (Don't change them for the purpose of this assignment)
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        //   If your mapper and combiner's  output types are different from Text.class,
        //   then uncomment the following lines to specify the data types.
        //job.setMapOutputKeyClass(?.class);
        //job.setMapOutputValueClass(?.class);

        // Input
        FileInputFormat.addInputPath(job, new Path(args[0]));
        job.setInputFormatClass(TextInputFormat.class);

        // Output
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        job.setOutputFormatClass(TextOutputFormat.class);

        // Execute job and return status
        return job.waitForCompletion(true) ? 0 : 1;
    }

    // Do not modify the main method
    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new TextAnalyzer(), args);
        System.exit(res);
    }

    // You may define sub-classes here. Example:
    // public static class MyClass {
    //
    // }
}
