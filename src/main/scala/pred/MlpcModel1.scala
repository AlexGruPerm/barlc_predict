package pred

import org.apache.spark.sql.{SparkSession, _}

/*
import org.apache.spark.sql.functions.round
import org.apache.spark.sql.cassandra._
import com.datastax.spark.connector._
import org.apache.spark.sql.types.DoubleType
*/

import org.apache.spark.ml.classification.MultilayerPerceptronClassifier
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.{SQLTransformer, StringIndexer, VectorAssembler}
import org.apache.spark.ml.{Pipeline, PipelineStage}
import org.slf4j.{Logger, LoggerFactory}
import scala.collection.mutable.ListBuffer

/*
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.classification.MultilayerPerceptronClassifier
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.{SQLTransformer, StringIndexer}
import org.apache.spark.ml.{Pipeline, PipelineStage}
import org.apache.spark.implicits._
import scala.collection.mutable.ListBuffer
import org.slf4j.{Logger, LoggerFactory}
*/

case class Form(res_type      :String,
                frmconfpeak   :String,
                sps           :Double,
                sms           :Double,
                tcvolprofile  :Int,
                acf_05_bws    :Double,
                acf_1_bws     :Double,
                acf_2_bws     :Double)

class MlpcModel1 {
  val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  logger.info("BEGIN Constructor "+getClass.getName)

  val spark = SparkSession.builder()
    .appName("barclpred")
    .config("spark.cassandra.connection.host","192.168.122.192")
    .config("spark.jars", "/root/barclpred.jar")
    .getOrCreate()

  import com.datastax.spark.connector.cql.CassandraConnectorConf
  import org.apache.spark.sql.cassandra._
  spark.setCassandraConf("Test Cluster", CassandraConnectorConf.ConnectionHostParam.option("192.168.122.192"))

  logger.info("SparkSession.builder() - Successful")

  def showLogs(ds :Dataset[Form]) = logger.info(" SIZE = "+ds.count)

  def getFormsDb(TickerID :Int, BarWidthSec: Int) :Dataset[Form] = {
    import org.apache.spark.sql.functions._
    import spark.implicits._
    spark.read.format("org.apache.spark.sql.cassandra")
      .options(Map("table" -> "bars_forms", "keyspace" -> "mts_bars"))
      .load()
      .where(col("ticker_id") === TickerID)
      .where(col("bar_width_sec") === BarWidthSec)
      .select(
        col("res_type"),
        col("formprops")("frmconfpeak").as("frmconfpeak"),
        col("formprops")("sps").as("sps").cast("Double"),
        col("formprops")("sms").as("sms").cast("Double"),
        col("formprops")("tcvolprofile").as("tcvolprofile").cast("Int"),
        col("formprops")("acf_05_bws").as("acf_05_bws").cast("Double"),
        col("formprops")("acf_1_bws").as("acf_1_bws").cast("Double"),
        col("formprops")("acf_2_bws").as("acf_2_bws").cast("Double")
      ).as[Form]}

  def readTrainData = {
    val ds :Dataset[Form] = Seq(1,3,5).map(elm => getFormsDb(elm,30)).reduce(_ union _)
    ds.cache()
    showLogs(ds)
    ds
  }

  val ds = readTrainData

  val splits = ds.randomSplit(Array(0.7, 0.3), seed = 1234L)
  val train = splits(0)
  val test = splits(1)

  def calcModel = {
    logger.info("begin build stages.")
    val stages = new ListBuffer[PipelineStage]()
    stages += new StringIndexer().setInputCol("frmconfpeak").setOutputCol("confpeakIndex")
    stages += new StringIndexer().setInputCol("tcvolprofile").setOutputCol("tcvolprofileIndex")
    stages += new StringIndexer().setInputCol("res_type").setOutputCol("label")
    stages += new VectorAssembler().setInputCols(Array("tcvolprofileIndex","sps","acf_1_bws","acf_2_bws","confpeakIndex")).setOutputCol("features")
    stages += new SQLTransformer().setStatement("SELECT label, features FROM __THIS__")

    val MLPCclassif = new MultilayerPerceptronClassifier().setLayers(Array[Int](5, 9, 5, 2)).setLabelCol("label").setFeaturesCol("features").setBlockSize(128).setSeed(1234L).setMaxIter(10)
    stages += MLPCclassif
    val estimator = new Pipeline().setStages(stages.toArray)
    val model = estimator.fit(train)
    val mlpc_predictions = model.transform(test)
    val predictionAndLabels = mlpc_predictions.select("prediction", "label")
    val evaluator = new MulticlassClassificationEvaluator().setMetricName("accuracy")
    val accur = evaluator.evaluate(predictionAndLabels)
    logger.info("accur by test data = "+accur)
    estimator.fit(ds)
  }

  val predModel = calcModel

  def getPredictionByModel:String = {
    logger.info("Call funciton getPredictionByModel")
    val inputData = ds.sample(false, 0.05).limit(1) //INPUT DS ONE ROW
    logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    logger.info("INSIDE getPredictionByModel")

   //    val tdFrmConfPeak  = inputData.select("frmconfpeak").first().getString(0)

    val tdForm  = inputData.first()

    logger.info("Input data is count="+inputData.count()+" tdForm="+tdForm)
    logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    val mlpc_predictions_res = predModel.transform(inputData)
    val predictionAndLabels = mlpc_predictions_res.select("prediction", "label")
    val evaluator = new MulticlassClassificationEvaluator().setMetricName("accuracy")
    val accur = evaluator.evaluate(predictionAndLabels)
    val predValue = predictionAndLabels.first().getDouble(0)
    val labelVal = predictionAndLabels.first().getDouble(1)
    "tdForm = "+tdForm+" predValue = "+predValue+" labelVal = "+labelVal+" accur = "+accur
  }

}
