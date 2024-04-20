Apologies for the large download, I wasn't sure how to ensure that you would have JavaFX installed to the right place
without just including it myself.
To run the project, use the buildandrun.ps1 script. It will make sure everything that needs to be included is included.
Just make sure to run it from the project directory, otherwise the relative paths I used to include JavaFX won't work right.
In VSCode, Ctrl-Shift-B should also work due to the tasks.json file I've created.

Also included in this repo is the updated Word doc, because there have been a couple of changes to it
(and the flowchart, to which there is a link in the doc) that are worth looking at.
Most notably, the explanation of what Conway's Game of Life is is better, and the flowchart has a new section
to represent the "edit properties" modal.