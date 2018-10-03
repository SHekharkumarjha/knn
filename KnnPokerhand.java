import java.io.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import java.net.URI;

import java.lang.Object;
import java.lang.Exception;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.WritableComparable;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;


/*
Attribute Information:

1) S1 "Suit of card #1"
Ordinal (1-4) representing {Hearts, Spades, Diamonds, Clubs}

2) C1 "Rank of card #1"
Numerical (1-13) representing (Ace, 2, 3, ... , Queen, King)

3) S2 "Suit of card #2"
4) C2 "Rank of card #2"

5) S3 "Suit of card #3"
6) C3 "Rank of card #3"

7) S4 "Suit of card #4"
8) C4 "Rank of card #4"

9) S5 "Suit of card #5"
10) C5 "Rank of card 5"

11) CLASS "Poker Hand"
Ordinal (0-9)

0: Nothing in hand; not a recognized poker hand
1: One pair; one pair of equal ranks within five cards
2: Two pairs; two pairs of equal ranks within five cards
3: Three of a kind; three equal ranks within five cards
4: Straight; five cards, sequentially ranked with no gaps
5: Flush; five cards with the same suit
6: Full house; pair + different rank three of a kind
7: Four of a kind; four equal ranks within five cards
8: Straight flush; straight + flush
9: Royal flush; {Ace, King, Queen, Jack, Ten} + flush 
*/


public class KnnPokerhand
{

	public static class KnnMapper extends Mapper<Object, Text, NullWritable, Text>
	{
		TreeMap<Double, String> KnnMap = new TreeMap<Double, String>();
		
		int K;
		
		double suit1;
		double suit2;
		double suit3;
		double suit4;
		double suit5;
		
		double rank1;
		double rank2;
		double rank3;
		double rank4;
		double rank5;
		
		double minSuit = 1;
		double maxSuit = 4;
		double minRank = 1;
		double maxRank = 13;

		private double normalisedDouble(String n1, double min, double max)
		{
			return (Double.parseDouble(n1) - min) / (max - min);

		}

		private double squaredDistance(double n1)
		{
			return Math.pow(n1,2);
		}

		private double totalSquaredDistance(double s1, double r1, double s2, double r2, double s3, double r3, double s4, double r4, double s5, double r5, double sR1, double rR1, double sR2, double rR2, double sR3, double rR3, double sR4, double rR4, double sR5, double rR5)
		{

				double s1Diff = s1 - sR1;
				double s2Diff = s2 - sR2;
				double s3Diff = s3 - sR3;
				double s4Diff = s4 - sR4;
				double s5Diff = s5 - sR5;

				double r1Diff = r1 - rR1;
				double r2Diff = r2 - rR2;
				double r3Diff = r3 - rR3;
				double r4Diff = r4 - rR4;
				double r5Diff = r5 - rR5;

				return (squaredDistance(s1Diff) + squaredDistance(s2Diff) + squaredDistance(s3Diff) + squaredDistance(s4Diff) + squaredDistance(s5Diff) + squaredDistance(r1Diff) + squaredDistance(r2Diff) + squaredDistance(r3Diff) + squaredDistance(r4Diff) + squaredDistance(r5Diff));

		}

		protected void setup(Context context) throws IOException, InterruptedException
		{
				
				Configuration conf = context.getConfiguration();
				String knnParams = conf.get("passedVal");

				StringTokenizer st = new StringTokenizer(knnParams, ",");

				suit1 = normalisedDouble(st.nextToken(), minSuit, maxSuit);
				rank1 = normalisedDouble(st.nextToken(), minRank, maxRank);

				suit2 = normalisedDouble(st.nextToken(), minSuit, maxSuit);
				rank2 = normalisedDouble(st.nextToken(), minRank, maxRank);

				suit3 = normalisedDouble(st.nextToken(), minSuit, maxSuit);
				rank3 = normalisedDouble(st.nextToken(), minRank, maxRank);

				suit4 = normalisedDouble(st.nextToken(), minSuit, maxSuit);
				rank4 = normalisedDouble(st.nextToken(), minRank, maxRank);

				suit5 = normalisedDouble(st.nextToken(), minSuit, maxSuit);
				rank5 = normalisedDouble(st.nextToken(), minRank, maxRank);

		}

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException
		{

				String rLine = value.toString();
				StringTokenizer st = new StringTokenizer(rLine, ",");

				double suit1R = normalisedDouble(st.nextToken(), minSuit, maxSuit);
				double rank1R = normalisedDouble(st.nextToken(), minRank, maxRank);

				double suit2R = normalisedDouble(st.nextToken(), minSuit, maxSuit);
				double rank2R = normalisedDouble(st.nextToken(), minRank, maxRank);

				double suit3R = normalisedDouble(st.nextToken(), minSuit, maxSuit);
				double rank3R = normalisedDouble(st.nextToken(), minRank, maxRank);

				double suit4R = normalisedDouble(st.nextToken(), minSuit, maxSuit);
				double rank4R = normalisedDouble(st.nextToken(), minRank, maxRank);

				double suit5R = normalisedDouble(st.nextToken(), minSuit, maxSuit);
				double rank5R = normalisedDouble(st.nextToken(), minRank, maxRank);

				String pokerClass = st.nextToken();

				double tDist = totalSquaredDistance(suit1, rank1, suit2, rank2, suit3, rank3, suit4, rank4, suit5, rank5, suit1R, rank1R, suit2R, rank2R, suit3R, rank3R, suit4R, rank4R, suit5R, rank5R);

				KnnMap.put(tDist, pokerClass);

				if (KnnMap.size() > K)
				{
					KnnMap.remove(KnnMap.lastKey());
				}

		}

		protected void cleanup(Context context) throws IOException, InterruptedException
		{

			List<String> knnList = new ArrayList<String>(KnnMap.values());

				Map<String, Integer> freqMap = new HashMap<String, Integer>();

				for(int i=0; i< knnList.size(); i++)
				{  
					Integer frequency = freqMap.get(knnList.get(i));
					if(frequency == null)
					{
						freqMap.put(knnList.get(i), 1);
					} else
					{
						freqMap.put(knnList.get(i), frequency+1);
					}
				}
				
				// Examine the HashMap to determine which key (model) has the highest value (frequency)
				String mostCommonClass = null;
				int maxFrequency = -1;
				for(Map.Entry<String, Integer> entry: freqMap.entrySet())
				{
					if(entry.getValue() > maxFrequency)
					{
						mostCommonClass = entry.getKey();
						maxFrequency = entry.getValue();
					}
				}
				
				try{
						context.write(NullWritable.get(), new Text(mostCommonClass));
					}
				catch(NullPointerException e)
				{
					Text nullOut = new Text();
					nullOut.set("None");

					context.write(NullWritable.get(), nullOut);
				}


		}
	}

	public static void main(String[] args) throws Exception 
	{
			// Create configuration
			Configuration conf = new Configuration();

			if (args.length != 3) 
			{
				System.err.println("Usage: KnnPattern <in> <out> <parameter file>");
				System.exit(2);
			}

			Path pt = new Path(args[2]);

			FileSystem fs = FileSystem.get(new Configuration());
			BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(pt)));

			String inputLine = "";

			int n = 1;
			while((inputLine = br.readLine()) != null)
			{

				conf.set("passedVal", inputLine);

				// Create job
				Job job = Job.getInstance(conf, "Find K-Nearest Neighbour #" + n);
				job.setJarByClass(KnnPokerhand.class);

				// Setup MapReduce job
				job.setMapperClass(KnnMapper.class);
				//job.setReducerClass(KnnReducer.class);
				job.setNumReduceTasks(0); // No reducer in this design

				// Specify key / value
				job.setMapOutputKeyClass(NullWritable.class);
				job.setMapOutputValueClass(Text.class);
				job.setOutputKeyClass(NullWritable.class);
				job.setOutputValueClass(IntWritable.class);

				// Input (the data file) and Output (the resulting classification)
				FileInputFormat.addInputPath(job, new Path(args[0]));
				FileOutputFormat.setOutputPath(job, new Path(args[1] + "_" + n));

				// Execute job
				final boolean jobSucceeded = job.waitForCompletion(true);

				if (!jobSucceeded) 
				{
					// return error status if job failed
					System.exit(1);
				}

				++n;
			}


	}

}
