package com.example.lucas.plotterbluetooth;

import java.io.Serializable;
import java.util.Date;

public class temperaturasBluetooth implements Serializable {
        private Date x;
        private float y;
        private float y2;

        public temperaturasBluetooth(Date x,float y,float y2) {
            this.x =x;
            this.y =y;
            this.y2=y2;
        }

        public Date getX() {
            return x;
        }
        public void setX(Date x) {
            this.x = x;
        }
        public float getY() {
            return y;
        }
        public void setY(int y) {
            this.y = y;
        }
        public float getY2() {
        return y2;
    }
        public void setY2(int y2) {
        this.y2 = y2;
    }
    }