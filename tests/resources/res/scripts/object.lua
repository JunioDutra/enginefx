return {
  update = function(delta)
    demo.object.move(delta)
  end,
  dispose = function()
    demo.object.disposed()
  end
}
