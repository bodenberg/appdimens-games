@tool
extends EditorPlugin

const AUTOLOAD := "AppDimens"

func _enable_plugin() -> void:
	add_autoload_singleton(AUTOLOAD, "res://addons/appdimens/appdimens.gd")

func _disable_plugin() -> void:
	remove_autoload_singleton(AUTOLOAD)
