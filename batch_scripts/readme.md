# README: Mother machine command line tools

In order to make this bash script running, copy MotherMachine_-<VERSION>.jar and
MMPreprocess_-<VERSION>.jar from `http://sites.imagej.net/MoMA/plugins/` into
this folder. You may also have to adapt the version number to the most recent
version in these files:
* `mmgui`
* `mmheadless`
* `mmpreprocess`

Furthermore, add the directory where this file is located to the PATH variable
of your system.

Afterwards, you may navigate to a folder, where a MoMA `growth_channel_folder`
is located and just execute

```
mkdir growth_channel_folder/analysis_output
mmheadless growth_channel_folder growth_channel_folder/analysis_output
```

This will make MoMA analyse the images in the folder and save the results to the
subfolder `output`
