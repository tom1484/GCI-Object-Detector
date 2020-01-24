# GCI-Object-Detector
An object detection Android app using TFLite.

## Results

<p align="center">
 <img src="src/img/result01.png" align="middle" width="300">
 <img src="src/img/result02.png" align="middle" width="300">
</p>

## Files explanation

<p align="center"><img src="src/img/structure.png" align="middle" width="300"></p>
* **MainActivity**
* Where **preview**,  **detector**, **processor** are declared and started.
  
* **CameraPreview**

  * This manages the preview of camera and passes it to the detector.
  
* **ObjectDetector**
* Receives frames from the preview and run the TFLite model. Finally, pass all recognitions to the processor.
  
* **ObjectProcessor**
* Print recognitions from the detector on the screen.
  
* **Recognition**
* Object to store recognitions.
  

## Model

 model name | input shape | quantized | model size |
|:-:|:-:|:-:|:-:|
 [COCO SSD MobileNet v1](http://download.tensorflow.org/models/object_detection/ssd_mobilenet_v1_coco_2018_01_28.tar.gz) | 1 x 300 x 300 x 3 | True | 27 Mb |

You can download the model [here](http://download.tensorflow.org/models/object_detection/ssd_mobilenet_v1_coco_2018_01_28.tar.gz).  After you extract it, there should be two files, **detect.tflite** and **labelmap.txt**. **detect.tflite** is where the model is stored. **labelmap.txt** contains all the classes the model can detect. <br/>

### Input format

The model takes an image as input which is **300 x 300** pixels with three channels per pixel. As the model is **quantized**, each value is a between **0** and **255**.

### Sample output

<p align="center"><img src="src/img/boundingbox.png" align="middle" width="300"></p>
The output contains **class**, **score** and **location**.

#### score (confidence)

Score, which can also be called as confidence, shows how sure about the result the model is.

#### location

Location indicates which box the object is inside and is represented by four values:

<p align="center"><strong>[   top,    left,    bottom,    right   ]</strong></p>
you can assume that they are equals to:

<p align="center"><strong>[   x1,    y1,    x2,    y2   ]</strong></p>

