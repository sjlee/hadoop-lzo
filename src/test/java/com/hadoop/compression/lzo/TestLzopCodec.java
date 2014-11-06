/*
 * This file is part of Hadoop-Gpl-Compression.
 *
 * Hadoop-Gpl-Compression is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Hadoop-Gpl-Compression is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hadoop-Gpl-Compression.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.hadoop.compression.lzo;

import junit.framework.TestCase;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextInputFormat;

public class TestLzopCodec extends TestCase {
  private String inputDataPath;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    inputDataPath = System.getProperty("test.build.data", "data");
  }

  public void testRaceCondition() throws Exception {
    JobConf conf = new JobConf();
    conf.set("io.compression.codecs", "com.hadoop.compression.lzo.LzopCodec");
    FileInputFormat.addInputPath(conf,
        new Path(inputDataPath + "/100.txt.lzo"));

    //CompressionCodecFactory codecFactory = new CompressionCodecFactory(conf);

    TextInputFormat inputFormat = new TextInputFormat();
    inputFormat.configure(conf);
    InputSplit[] splits = inputFormat.getSplits(conf, 1);

    for (InputSplit is: splits) {
      RecordReader<LongWritable, Text> rr =
          inputFormat.getRecordReader(is, conf, Reporter.NULL);
      LongWritable key = rr.createKey();
      Text value = rr.createValue();
      while (rr.next(key, value)) {
        System.out.println("Read: " + key + ", " + value);
      }
      // This call to rr.close() will put the SAME decompressor into the pool
      // 2x!
      rr.close();
    }

    // These guys are both going to get the same instance of the decompressor!
    // Yikes!

    TextInputFormat inputFormat2 = new TextInputFormat();
    inputFormat.configure(conf);

    TextInputFormat inputFormat3 = new TextInputFormat();
    inputFormat.configure(conf);

    for (InputSplit is: splits) {
      RecordReader<LongWritable, Text> rr2 =
          inputFormat2.getRecordReader(is, conf, Reporter.NULL);
      RecordReader<LongWritable, Text> rr3 =
          inputFormat3.getRecordReader(is, conf, Reporter.NULL);

      LongWritable key2 = rr2.createKey();
      Text value2 = rr2.createValue();
      LongWritable key3 = rr3.createKey();
      Text value3 = rr3.createValue();
      while (rr2.next(key2, value2) && rr3.next(key3, value3)) {
        System.out.println("IS2: " + key2 + ", " + value2 + " IS3: " + key3 +
            ", " + value3);
      }
    }
  }
}