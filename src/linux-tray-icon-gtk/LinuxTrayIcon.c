#include <jni.h>
#include <stdio.h>
#include <gtk/gtk.h>
#include <pthread.h>
#include "LinuxTrayIcon.h"

JNIEnv *env;
jobject javaObject;
GtkStatusIcon *trayIcon;

void activate ( GtkStatusIcon *statusIcon, GdkEvent *event, gpointer user_data ) {
   jclass thisClass = (*env)->GetObjectClass ( env, javaObject );
   jmethodID callback = (*env)->GetMethodID( env, thisClass, "jni_requestToggleUI", "()V" );
   (*env)->CallVoidMethod(env, javaObject, callback);
}

void requestPlay ( GtkStatusIcon *statusIcon, GdkEvent *event, gpointer user_data ) {
   jclass thisClass = (*env)->GetObjectClass ( env, javaObject );
   jmethodID callback = (*env)->GetMethodID( env, thisClass, "jni_requestPlay", "()V" );
   (*env)->CallVoidMethod(env, javaObject, callback);
}

void requestPause ( GtkStatusIcon *statusIcon, GdkEvent *event, gpointer user_data ) {
   jclass thisClass = (*env)->GetObjectClass ( env, javaObject );
   jmethodID callback = (*env)->GetMethodID( env, thisClass, "jni_requestPause", "()V" );
   (*env)->CallVoidMethod(env, javaObject, callback);
}

void requestPrevious ( GtkStatusIcon *statusIcon, GdkEvent *event, gpointer user_data ) {
   jclass thisClass = (*env)->GetObjectClass ( env, javaObject );
   jmethodID callback = (*env)->GetMethodID( env, thisClass, "jni_requestPrevious", "()V" );
   (*env)->CallVoidMethod(env, javaObject, callback);
}

void requestNext ( GtkStatusIcon *statusIcon, GdkEvent *event, gpointer user_data ) {
   jclass thisClass = (*env)->GetObjectClass ( env, javaObject );
   jmethodID callback = (*env)->GetMethodID( env, thisClass, "jni_requestNext", "()V" );
   (*env)->CallVoidMethod(env, javaObject, callback);
}

void requestStop ( GtkStatusIcon *statusIcon, GdkEvent *event, gpointer user_data ) {
   jclass thisClass = (*env)->GetObjectClass ( env, javaObject );
   jmethodID callback = (*env)->GetMethodID( env, thisClass, "jni_requestStop", "()V" );
   (*env)->CallVoidMethod(env, javaObject, callback);
}

void requestMute ( GtkStatusIcon *statusIcon, GdkEvent *event, gpointer user_data ) {
   jclass thisClass = (*env)->GetObjectClass ( env, javaObject );
   jmethodID callback = (*env)->GetMethodID( env, thisClass, "jni_requestMute", "()V" );
   (*env)->CallVoidMethod(env, javaObject, callback);
}

void requestExit ( GtkStatusIcon *statusIcon, GdkEvent *event, gpointer user_data ) {
   jclass thisClass = (*env)->GetObjectClass ( env, javaObject );
   jmethodID callback = (*env)->GetMethodID( env, thisClass, "jni_requestExit", "()V" );
   (*env)->CallVoidMethod(env, javaObject, callback);
}

void tellJavaDoneInit() {
   jclass thisClass = (*env)->GetObjectClass ( env, javaObject );
   jmethodID callback = (*env)->GetMethodID( env, thisClass, "jni_doneInit", "()V" );
   (*env)->CallVoidMethod(env, javaObject, callback);
}

static void showMenu(GtkStatusIcon *status_icon, guint button, guint32 activate_time, gpointer popUpMenu) {
   #pragma GCC diagnostic ignored "-Wdeprecated-declarations"
    gtk_menu_popup(GTK_MENU(popUpMenu), NULL, NULL, gtk_status_icon_position_menu, status_icon, button, activate_time);
}

JNIEXPORT void JNICALL Java_net_joshuad_hypnos_trayicon_LinuxTrayIcon_initTrayIcon (JNIEnv *env_in, jobject thisObject,
jstring iconLocationJava ) {
   env = env_in;
   javaObject = thisObject;

   gtk_init ( NULL, NULL );

   //set popup menu for tray icon
   GtkWidget *menu;
   GtkWidget *menuItemToggleUI, *menuItemPlay, *menuItemPause, *menuItemNext, *menuItemPrevious, *menuItemStop, 
      *menuItemMute, *menuItemExit;
   menu = gtk_menu_new();
   
   menuItemToggleUI = gtk_menu_item_new_with_label( "Show/Hide" );
   g_signal_connect ( G_OBJECT (menuItemToggleUI), "activate", G_CALLBACK ( activate ), NULL );
   gtk_menu_shell_append ( GTK_MENU_SHELL (menu), menuItemToggleUI );

   gtk_menu_shell_append (GTK_MENU_SHELL (menu), gtk_separator_menu_item_new() );

   menuItemPlay = gtk_image_menu_item_new_from_stock( GTK_STOCK_MEDIA_PLAY, NULL );
   g_signal_connect ( G_OBJECT (menuItemPlay), "activate", G_CALLBACK ( requestPlay ), NULL );
   gtk_menu_shell_append ( GTK_MENU_SHELL (menu), menuItemPlay );

   menuItemPause = gtk_image_menu_item_new_from_stock( GTK_STOCK_MEDIA_PAUSE, NULL );
   g_signal_connect ( G_OBJECT (menuItemPause), "activate", G_CALLBACK ( requestPause ), NULL );
   gtk_menu_shell_append ( GTK_MENU_SHELL (menu), menuItemPause );

   menuItemNext = gtk_image_menu_item_new_from_stock( GTK_STOCK_MEDIA_NEXT, NULL );
   g_signal_connect ( G_OBJECT (menuItemNext), "activate", G_CALLBACK ( requestNext ), NULL );
   gtk_menu_shell_append ( GTK_MENU_SHELL (menu), menuItemNext );

   menuItemPrevious = gtk_image_menu_item_new_from_stock( GTK_STOCK_MEDIA_PREVIOUS, NULL );
   g_signal_connect ( G_OBJECT (menuItemPrevious), "activate", G_CALLBACK ( requestPrevious ), NULL );
   gtk_menu_shell_append ( GTK_MENU_SHELL (menu), menuItemPrevious );

   menuItemStop = gtk_image_menu_item_new_from_stock( GTK_STOCK_MEDIA_STOP, NULL );
   g_signal_connect ( G_OBJECT (menuItemStop), "activate", G_CALLBACK ( requestStop ), NULL );
   gtk_menu_shell_append ( GTK_MENU_SHELL (menu), menuItemStop );

   menuItemMute = gtk_menu_item_new_with_label( "Mute / Unmute" );
   g_signal_connect ( G_OBJECT (menuItemMute), "activate", G_CALLBACK ( requestMute ), NULL );
   gtk_menu_shell_append ( GTK_MENU_SHELL (menu), menuItemMute );

   gtk_menu_shell_append (GTK_MENU_SHELL (menu), gtk_separator_menu_item_new() );

   menuItemExit = gtk_image_menu_item_new_from_stock( GTK_STOCK_QUIT, NULL );
   g_signal_connect ( G_OBJECT (menuItemExit), "activate", G_CALLBACK ( requestExit ), NULL );
   gtk_menu_shell_append (GTK_MENU_SHELL (menu), menuItemExit);

   gtk_widget_show_all (menu);

   #pragma GCC diagnostic ignored "-Wdeprecated-declarations"
   const char *iconLocation = (*env)->GetStringUTFChars(env, iconLocationJava, NULL);
   trayIcon = gtk_status_icon_new_from_file ( iconLocation );
   g_signal_connect ( trayIcon, "activate", G_CALLBACK ( activate ), NULL );
   g_signal_connect ( GTK_STATUS_ICON ( trayIcon ), "popup-menu", GTK_SIGNAL_FUNC (showMenu), menu );

   tellJavaDoneInit();

   gtk_main();

   return;
}

JNIEXPORT void JNICALL Java_net_joshuad_hypnos_trayicon_LinuxTrayIcon_showTrayIcon (JNIEnv *env, jobject thisObject ) {
   gtk_status_icon_set_visible ( trayIcon, TRUE );
}

JNIEXPORT void JNICALL Java_net_joshuad_hypnos_trayicon_LinuxTrayIcon_hideTrayIcon (JNIEnv *env, jobject thisObject ) {
   gtk_status_icon_set_visible ( trayIcon, FALSE );
}

