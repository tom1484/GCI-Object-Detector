package com.example.gciobjectdetector;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Size;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;

public class ObjectDetector {

    private Context context;
    private ObjectProcessor mObjectProcessor;
    private Size previewSize;

    private static final int NUM_DETECTIONS = 10;
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    private static final int NUM_THREADS = 4;
    private boolean isModelQuantized;
    private int inputSize = 300;
    private Vector<String> labels = new Vector<String>();
    private int[] intValues;
    private float[][][] outputLocations;
    private float[][] outputClasses;
    private float[][] outputScores;
    private float[] numDetections;

    private ByteBuffer imgData;

    private Interpreter tfLite;

    private String modelFilename = "detect.tflite";
    private String labelFilename = "labelmap.txt";
    private AssetManager assetManager;

    private int cropSize = 300;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private Canvas canvas;
    private Bitmap croppedFrame;

    private Queue<Runnable> recognitionJobs;
    private List<Recognition> recognitions;

    public ObjectDetector(Context ctx, Size previewSize) throws IOException {
        context = ctx;
        this.previewSize = previewSize;
        assetManager = context.getAssets();

        // load labels
        InputStream labelsInput = null;
        labelsInput = assetManager.open(labelFilename);
        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
            labels.add(line);
        }
        br.close();

        // create interpreter
        try {
            tfLite = new Interpreter(loadModelFile(assetManager, modelFilename));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // create buffer for frame data
        isModelQuantized = true;
        int numBytesPerChannel;
        if (isModelQuantized) {
            numBytesPerChannel = 1;
        } else {
            numBytesPerChannel = 4;
        }
        imgData = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * numBytesPerChannel);
        imgData.order(ByteOrder.nativeOrder());


        // store every pixel of frame
        intValues = new int[inputSize * inputSize];

  		// create output content
        tfLite.setNumThreads(NUM_THREADS);
        outputLocations = new float[1][NUM_DETECTIONS][4];
        outputClasses = new float[1][NUM_DETECTIONS];
        outputScores = new float[1][NUM_DETECTIONS];
        numDetections = new float[1];

        croppedFrame = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        // transform frames to required shape
        frameToCropTransform =
                getTransformationMatrix(
                        previewSize.getWidth(), previewSize.getHeight(),
                        cropSize, cropSize);
        cropToFrameTransform = new Matrix();
        // transform cropped frames to original shape
        frameToCropTransform.invert(cropToFrameTransform);

        // create new thread for detection
        recognitionJobs = new LinkedList<Runnable>();
        recognitions = new LinkedList<Recognition>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    Runnable job = null;
                    while (recognitionJobs.peek() != null) {
                        job = recognitionJobs.poll();
                    }
                    if (job != null) {
                        job.run();
                    }
                }
            }
        }).start();

    }

    public void setObjectProcessor(ObjectProcessor mObjectProcessor) {
        this.mObjectProcessor = mObjectProcessor;
    }

    public void detect(Bitmap frame) {

    	// crop frame and store pixels
        canvas = new Canvas(croppedFrame);
        canvas.drawBitmap(frame, frameToCropTransform, null);
        croppedFrame.getPixels(intValues, 0, croppedFrame.getWidth(), 0, 0, croppedFrame.getWidth(), croppedFrame.getHeight());

        // add new detection job
        recognitionJobs.add(new Runnable() {
            @Override
            public void run() {
                recognitions = recognizeImage();
            }
        });
        mObjectProcessor.process(recognitions, cropToFrameTransform);
    }

    public List<Recognition> recognizeImage() {

        imgData.rewind();
        // load every byte of frame
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                // 0xRRGGBB
                if (isModelQuantized) {
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else {
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }

        outputLocations = new float[1][NUM_DETECTIONS][4];
        outputClasses = new float[1][NUM_DETECTIONS];
        outputScores = new float[1][NUM_DETECTIONS];
        numDetections = new float[1];

        Object[] inputArray = {imgData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputLocations);
        outputMap.put(1, outputClasses);
        outputMap.put(2, outputScores);
        outputMap.put(3, numDetections);

        // run detection
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);

        // read recognition
        final ArrayList<Recognition> recognitions = new ArrayList<>(NUM_DETECTIONS);
        for (int i = 0; i < NUM_DETECTIONS; ++i) {
            final RectF detection =
                    new RectF(
                            outputLocations[0][i][1] * inputSize,
                            outputLocations[0][i][0] * inputSize,
                            outputLocations[0][i][3] * inputSize,
                            outputLocations[0][i][2] * inputSize);

            int labelOffset = 1;
            recognitions.add(
                    new Recognition(i, labels.get((int) outputClasses[0][i] + labelOffset), outputScores[0][i], detection));
        }

        return recognitions;
    }

    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename) throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public Matrix getTransformationMatrix(int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
        Matrix matrix = new Matrix();

        if (srcWidth != dstWidth || srcHeight != dstHeight) {
            final float scaleFactorX = dstWidth / (float) srcWidth;
            final float scaleFactorY = dstHeight / (float) srcHeight;

            matrix.postScale(scaleFactorX, scaleFactorY);
        }

        return matrix;
    }
}
