# MoMA
The **Mo**ther **M**achine **A**nalyzer.

## Abstract
The Mother Machine is a microfluidic device designed to study e.g. bacteria. 
It allows the observation of growth and division of the progeny of single “mother” cells over many generations using time lapse microscopy.
Individual cells need to be tracked over time. 
Tracking consist of two equally important tasks: 
*(i)* cells need to be segmented in each frame, and 
*(ii)* all segments of the same cell need to be linked between frames. 
Tracking large numbers of cells under different environmental conditions will allow biologists to better understand the stochastic 
dynamics of gene expression within living cells. 
Respective high throughput studies of cells in the Mother Machine would be greatly facilitated if the tracking task would be automated.

The tracking model we use is built on the basis of a large set of cell segmentation hypotheses for each video frame. 
Possible tracking *assignments* between segments across time, including cell identity mapping, cell division, and cell exit events 
are enumerated. Each such assignment is represented as a binary decision variable with unary costs based on image and object features 
of the involved segments. We find a cost-minimal and consistent solution (maximum a posteriori) by solving an integer linear program 
using [Gurobi](http://www.gurobi.com).

MoMA offers six innovative ways for semi-automatic curation of automatically found tracking solutions. 
We show how all proposed interactions can be elegantly incorporated into the assignment tracking model mentioned above.
After interactively pointing at a single mistake, multiple segmentation and tracking errors are fixed automatically in one single
reevaluation of the model.

## Publications
[Optimal Joint Segmentation and Tracking of Escherichia Coli in the Mother Machine](http://link.springer.com/chapter/10.1007/978-3-319-12289-2_3#page-1)
F Jug, T Pietzsch, D Kainmüller, J Funke, M Kaiser, E van Nimwegen, G Myers
BAMBI@MICCAI, 2014

[Tracking by Assignment Facilitates Data Curation](https://www.researchgate.net/profile/Florian_Jug/publication/265850602_Tracking_by_Assignment_Facilitates_Data_Curation/links/541f16b60cf2218008d3e3a5.pdf)
F Jug, T Pietzsch, D Kainmüller, G Myers
IMIC@MICCAI, 2014

## Installation

1. Download Gurobi binary in [Gurobi Download Center](http://www.gurobi.com/downloads/download-center) 
  - The current version is 6.5.2 supporting Windows 32/64, Linux 64, MacOSX 64, AIX 64 architectures
2. Get the right license for you from [Gurobi](http://www.gurobi.com)
3. Install & run MoMA
## Wiki
Find more information [here](https://github.com/fjug/MoMA/wiki).
