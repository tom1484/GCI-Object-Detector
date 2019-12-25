# GCI-Object-Detector
An Object Dtection App using TFLite for Google Code-in

## Model
 model name | input shape | quantized | model size |
|:-:|:-:|:-:|:-:|
 [COCO SSD MobileNet v1](http://download.tensorflow.org/models/object_detection/ssd_mobilenet_v1_coco_2018_01_28.tar.gz) | 1 x 300 x 300 x 3 | True | 27 Mb |

You can download the model [here](http://download.tensorflow.org/models/object_detection/ssd_mobilenet_v1_coco_2018_01_28.tar.gz).  After you extract it, there should be two files, **detect.tflite** and **labelmap.txt**. **detect.tflite** is where the model is stored. **labelmap.txt** contains all the classes the model can detect. <br/>

### Input
The model takes an image as input which is **300 x 300** pixels with three channels per pixel. As the model is **quantized**, each value is a between **0** and **255**.

### Output
