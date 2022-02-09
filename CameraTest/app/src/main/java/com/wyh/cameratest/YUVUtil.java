package com.wyh.cameratest;

public class YUVUtil {
    public static void yuvToNv12(byte[] y, byte[] u, byte[] v, byte[] nv12, int stride, int height) {
        System.arraycopy(y, 0, nv12, 0, y.length);
        // 注意，若length值为 y.length * 3 / 2 会有数组越界的风险，需使用真实数据长度计算
        int length = y.length + (u.length  + v.length) / 2;
        int uIndex = 0, vIndex = 0;
        for (int i = stride * height; i < length; i += 2) {
            nv12[i] = u[vIndex];
            nv12[i + 1] = v[uIndex];
            vIndex += 2;
            uIndex += 2;
        }
    }
}
