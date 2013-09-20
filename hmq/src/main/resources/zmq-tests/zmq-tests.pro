#-------------------------------------------------
#
# Project created by QtCreator 2013-09-17T13:50:31
#
#-------------------------------------------------

QT       += core

QT       -= gui

TARGET = zmq-tests
CONFIG   += console
CONFIG   -= app_bundle
LIBS += -lzmq

TEMPLATE = app


SOURCES += main.cpp \
    sub.c \
    pub.c \
    push.c

HEADERS += \
    zhelpers.h
