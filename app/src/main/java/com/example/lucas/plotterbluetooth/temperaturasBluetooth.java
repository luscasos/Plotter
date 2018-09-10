package com.example.lucas.plotterbluetooth;

import java.io.Serializable;
import java.util.Date;

public class temperaturasBluetooth implements Serializable {
        private Date x;
        private float y;

        public temperaturasBluetooth(Date x,float y) {
            this.x =x;
            this.y =y;
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
    }