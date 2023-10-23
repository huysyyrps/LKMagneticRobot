package com.example.lkmagneticrobot.constant

object Constant {
    const val SAVE_IMAGE_PATH = "LKImage"
    const val SAVE_VIDEO_PATH = "LKVideo"
    const val TAG_ONE = 1
    const val TAG_TWO = 2
    const val URL = "http://192.168.43.251:8080"
    const val STATUS_CONNECT_ERROR = -1
    const val STATUS_CONNECT_CLOSED = 0
    const val STATUS_CONNECT_SUCCESS = 1
    const val CONTROLLERIP = "192.168.144.108"
    const val CONTROLLERPORT = 5001
    const val LDGOPEN = "AT+LED -e1\n"
    const val CLOSE = "AT+LED -e0\n"

    const val SERVERIP = "192.168.144.101"
    //TCP端口   14550对应uart0   14551对应uart1
    const val SERVERPORT = 14551
    const val FPVURL = "rtsp://192.168.144.108:554/stream=0"
}