# Coinselection

This library provides UTXO selection algorithm for BTC.

 **Random-Improve** algorithm (credits to https://iohk.io/blog/self-organisation-in-coin-selection/). 

### Main features:
1. Randomly picks unspent outputs from the provided list until the target value is covered. In case the algorithm has picked too many inputs (default 60) - fallback to Largest first algorithm.
2. Improve step: take remaining inputs until reaching 2*target, so that the change output is close to target. This creates useful UTXOs in the long run, especially if we tend to send approximately the same amount. That is the main point of this algorithm. 

This algorithm is fairly straightforward, has high performance due to random nature (unlike algorithms based on dynamic programming) and decreases transaction costs. The original article provides more insight on this algorithm.

### Install:
Add this project as library using jitpack:

In `build.gradle` add:

    allprojects {
        repositories {
            maven { url 'https://jitpack.io' }
        }
    }
 Then, in your dependencies add:
 
    implementation 'com.github.vladmelnyk:coinselection:0.0.3' 
    
For the latest version please refer to https://jitpack.io/#vladmelnyk/coinselection

 
Feel free to contribute :)