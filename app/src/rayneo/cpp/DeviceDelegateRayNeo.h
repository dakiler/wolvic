/* -*- Mode: C++; tab-width: 20; indent-tabs-mode: nil; c-basic-offset: 2 -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#ifndef DEVICE_DELEGATE_NO_API_DOT_H
#define DEVICE_DELEGATE_NO_API_DOT_H

#include "vrb/Forward.h"
#include "vrb/MacroUtils.h"
#include "DeviceDelegate.h"

#include <jni.h>
#include <memory>

namespace crow {

class DeviceDelegateRayNeo;
typedef std::shared_ptr<DeviceDelegateRayNeo> DeviceDelegateRayNeoPtr;

class DeviceDelegateRayNeo : public DeviceDelegate {
public:
  static DeviceDelegateRayNeoPtr Create(vrb::RenderContextPtr& aContext);
  // DeviceDelegate interface
  void SetRenderMode(const device::RenderMode aMode) override;
  device::RenderMode GetRenderMode() override;
  void RegisterImmersiveDisplay(ImmersiveDisplayPtr aDisplay) override;
  GestureDelegateConstPtr GetGestureDelegate() override;
  vrb::CameraPtr GetCamera(const device::Eye) override;
  const vrb::Matrix& GetHeadTransform() const override;
  const vrb::Matrix& GetReorientTransform() const override;
  void SetReorientTransform(const vrb::Matrix& aMatrix) override;
  void SetClearColor(const vrb::Color& aColor) override;
  void SetClipPlanes(const float aNear, const float aFar) override;
  void SetControllerDelegate(ControllerDelegatePtr& aController) override;
  void ReleaseControllerDelegate() override;
  int32_t GetControllerModelCount() const override;
  const std::string GetControllerModelName(const int32_t aModelIndex) const override;
  void ProcessEvents() override;
  void StartFrame(const FramePrediction aPrediction) override;
  void BindEye(const device::Eye) override;
  void EndFrame(const FrameEndMode aMode) override;
  // DeviceDelegateRayNeo interface
  void InitializeJava(JNIEnv* aEnv, jobject aActivity);
  void ShutdownJava();
  void SetViewport(const int aWidth, const int aHeight);
  void Pause();
  void Resume();
  void MoveAxis(const float aX, const float aY, const float aZ);
  void RotateHeading(const float aHeading);
  void RotatePitch(const float aPitch);
  void TouchEvent(const bool aDown, const float aX, const float aY);
  void ControllerButtonPressed(const bool aDown);
  //Kaidi
  void SetDeviceQuaternion(float f0, float f1, float f2, float f3);
  void SetControllerQuaternion(float f0, float f1, float f2, float f3);
  void UpdateController();

protected:
  struct State;
  DeviceDelegateRayNeo(State& aState);
  virtual ~DeviceDelegateRayNeo();
private:
  State& m;
  float controllerQua[4];
  VRB_NO_DEFAULTS(DeviceDelegateRayNeo)
};

} // namespace crow
#endif // DEVICE_DELEGATE_NO_API_DOT_H
