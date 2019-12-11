# Introduction

Mobile Analysis ToolKit provides a set of commands to simplify analysis mobile applications.
It also provides tools to decompile/recompile, and manipulate Android applications.

*NOTE: This is a widely Work In Progress project*

# Installation

1. Clone the project
2. Run the following command to build the toolkit : `./gradlew installDist`
3. Add generated bin folder to your `PATH` environment variable : `PATH="$PATH":"<path_to_project>/build/install/matk/bin"` 
4. (optional) Register auto-complete script to your Shell : ``

## Auto-complete

You can add Bash auto-completion on Matk.

1. Generate the appropriate script with `_MATK_COMPLETE=zsh matk > ~/.matk/auto-complete.sh`
2. **source** the generated script on your `.<shell>rc` file (`.bashrc`, `.zshrc`, ...) : `echo source ~/.matk/auto-complete.sh >> ~/.bashrc`
