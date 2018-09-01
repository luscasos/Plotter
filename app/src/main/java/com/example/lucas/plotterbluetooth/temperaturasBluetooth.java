package com.example.lucas.plotterbluetooth;

import java.io.Serializable;

public class temperaturasBluetooth implements Serializable {
        private float x;
        private float y;

        public temperaturasBluetooth(float x,float y) {
            this.x =x;
            this.y =y;
        }

        public float getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }
        public float getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }
    }